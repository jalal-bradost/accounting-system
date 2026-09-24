package com.bradox.erp.purchase.service.domain;

import com.bradox.erp.inventory.domain.core.entity.Product;
import com.bradox.erp.inventory.domain.core.entity.ProductPackaging;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.domain.core.valueobject.ProductType;
import com.bradox.erp.inventory.service.domain.ports.input.UomApplicationService;
import com.bradox.erp.inventory.service.domain.ports.output.StockMovePurchaseQueryPort;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductRepository;
import com.bradox.erp.platform.activity.RecordActivityLogger;
import com.bradox.erp.purchase.domain.core.entity.PurchaseOrder;
import com.bradox.erp.purchase.domain.core.entity.PurchaseOrderLine;
import com.bradox.erp.purchase.service.domain.ports.output.repository.PurchaseOrderRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies PO qty updates and retries if another writer
 * (Rabbit receive-sync vs bill post) already incremented {@code row_version}.
 */
@Component
public class PurchaseOrderQtyWriter {

    private static final int MAX_ATTEMPTS = 8;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockMovePurchaseQueryPort stockMovePurchaseQueryPort;
    private final ProductRepository productRepository;
    private final UomApplicationService uomApplicationService;
    private final RecordActivityLogger activityLogger;

    public PurchaseOrderQtyWriter(PurchaseOrderRepository purchaseOrderRepository,
                                  StockMovePurchaseQueryPort stockMovePurchaseQueryPort,
                                  ProductRepository productRepository,
                                  UomApplicationService uomApplicationService,
                                  RecordActivityLogger activityLogger) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.stockMovePurchaseQueryPort = stockMovePurchaseQueryPort;
        this.productRepository = productRepository;
        this.uomApplicationService = uomApplicationService;
        this.activityLogger = activityLogger;
    }

    public PurchaseOrder updateQtyReceived(UUID purchaseOrderId) {
        return retry(() -> doUpdateQtyReceived(purchaseOrderId));
    }

    /** Same as {@link #updateQtyReceived} but joins the caller transaction (PO finalize-on-create). */
    public PurchaseOrder updateQtyReceivedJoiningCurrentTransaction(UUID purchaseOrderId) {
        return doUpdateQtyReceived(purchaseOrderId);
    }

    public void applyPostedBillQuantities(UUID purchaseOrderId,
                                          boolean creditNote,
                                          List<PostedBillLineQty> lines) {
        // Join the caller transaction. Nested REQUIRES_NEW deadlocks when the outer TX already
        // holds a pessimistic lock on the same purchase order (finalize-on-create).
        retry(() -> {
            doApplyPostedBillQuantities(purchaseOrderId, creditNote, lines);
            return null;
        });
    }

    private PurchaseOrder doUpdateQtyReceived(UUID purchaseOrderId) {
        PurchaseOrder o = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId).orElse(null);
        if (o == null) {
            return null;
        }
        Map<UUID, BigDecimal> before = new HashMap<>();
        for (PurchaseOrderLine line : o.getLines()) {
            before.put(line.getId(), nz(line.getQtyReceived()));
        }
        Instant now = Instant.now();
        for (PurchaseOrderLine line : o.getLines()) {
            BigDecimal sumBase = stockMovePurchaseQueryPort.sumPickedQuantityForPurchaseOrderLine(line.getId());
            line.setQtyReceived(toDocumentQty(line, sumBase).setScale(4, RoundingMode.HALF_UP));
            line.setUpdatedAt(now);
        }
        boolean allReceived = o.getLines().stream().allMatch(l -> {
            Optional<Product> p = productRepository.findById(new ProductId(l.getProductId()));
            if (p.isEmpty() || p.get().getProductType() == ProductType.SERVICE) {
                return true;
            }
            return l.getQtyReceived().compareTo(l.getQtyOrdered()) >= 0;
        });
        if (allReceived) {
            o.setReceivedCompletedAt(Instant.now());
        }
        o.setUpdatedAt(now);
        PurchaseOrder saved = purchaseOrderRepository.save(o);
        postReceivedQtyTracking(saved, before);
        return saved;
    }

    private void postReceivedQtyTracking(PurchaseOrder o, Map<UUID, BigDecimal> before) {
        List<String> blocks = new ArrayList<>();
        for (PurchaseOrderLine line : o.getLines()) {
            BigDecimal oldQty = before.getOrDefault(line.getId(), BigDecimal.ZERO);
            BigDecimal newQty = nz(line.getQtyReceived());
            if (oldQty.compareTo(newQty) == 0) {
                continue;
            }
            blocks.add("• " + productLabel(line) + ":\n  Received Quantity: "
                    + formatQty(oldQty) + " → " + formatQty(newQty));
        }
        if (blocks.isEmpty()) {
            return;
        }
        activityLogger.log(
                o.getCompanyId(),
                RecordActivityLogger.MODEL_PURCHASE_ORDER,
                o.getId(),
                "The received quantity has been updated.\n" + String.join("\n", blocks));
    }

    private String productLabel(PurchaseOrderLine line) {
        Optional<Product> p = productRepository.findById(new ProductId(line.getProductId()));
        if (p.isPresent()) {
            return "[" + p.get().getSku() + "] " + p.get().getName();
        }
        if (line.getName() != null && !line.getName().isBlank()) {
            return line.getName();
        }
        String id = line.getProductId().toString();
        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String formatQty(BigDecimal v) {
        return nz(v).stripTrailingZeros().toPlainString();
    }

    private void doApplyPostedBillQuantities(UUID purchaseOrderId,
                                             boolean creditNote,
                                             List<PostedBillLineQty> lines) {
        PurchaseOrder po = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId).orElse(null);
        if (po == null) {
            return;
        }
        Map<UUID, BigDecimal> before = new LinkedHashMap<>();
        for (PurchaseOrderLine line : po.getLines()) {
            before.put(line.getId(), nz(line.getQtyInvoiced()));
        }
        BigDecimal sign = creditNote ? BigDecimal.ONE.negate() : BigDecimal.ONE;
        Instant now = Instant.now();
        for (PostedBillLineQty vbl : lines) {
            if (vbl.purchaseOrderLineId() == null) {
                continue;
            }
            po.getLines().stream()
                    .filter(l -> l.getId().equals(vbl.purchaseOrderLineId()))
                    .findFirst()
                    .ifPresent(pol -> {
                        BigDecimal next = pol.getQtyInvoiced().add(vbl.qty().multiply(sign))
                                .setScale(4, RoundingMode.HALF_UP);
                        if (next.signum() < 0) {
                            next = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                        }
                        pol.setQtyInvoiced(next);
                        pol.setUpdatedAt(now);
                    });
        }
        boolean allBilled = po.getLines().stream().allMatch(l ->
                l.getQtyInvoiced().compareTo(l.getQtyOrdered()) >= 0);
        if (allBilled) {
            po.setBilledCompletedAt(now);
        }
        po.setUpdatedAt(now);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        postBilledQtyTracking(saved, before);
    }

    private void postBilledQtyTracking(PurchaseOrder o, Map<UUID, BigDecimal> before) {
        List<String> blocks = new ArrayList<>();
        for (PurchaseOrderLine line : o.getLines()) {
            BigDecimal oldQty = before.getOrDefault(line.getId(), BigDecimal.ZERO);
            BigDecimal newQty = nz(line.getQtyInvoiced());
            if (oldQty.compareTo(newQty) == 0) {
                continue;
            }
            blocks.add("• " + productLabel(line) + ":\n  Billed Quantity: "
                    + formatQty(oldQty) + " → " + formatQty(newQty));
        }
        if (blocks.isEmpty()) {
            return;
        }
        activityLogger.log(
                o.getCompanyId(),
                RecordActivityLogger.MODEL_PURCHASE_ORDER,
                o.getId(),
                "The billed quantity has been updated.\n" + String.join("\n", blocks));
    }

    /** Convert picked base-UOM quantity into the document line unit (packaging or line UOM). */
    private BigDecimal toDocumentQty(PurchaseOrderLine line, BigDecimal baseQty) {
        if (baseQty == null) {
            return BigDecimal.ZERO;
        }
        if (line.getQtyPerPackage() != null && line.getQtyPerPackage().signum() > 0) {
            return ProductPackaging.fromBaseQty(baseQty, line.getQtyPerPackage());
        }
        Optional<Product> product = productRepository.findById(new ProductId(line.getProductId()));
        if (product.isEmpty() || line.getUomId() == null) {
            return baseQty;
        }
        UUID stockUom = product.get().getUomId().getId();
        if (stockUom.equals(line.getUomId())) {
            return baseQty;
        }
        return uomApplicationService.convert(stockUom, line.getUomId(), baseQty);
    }

    private <T> T retry(java.util.function.Supplier<T> action) {
        OptimisticLockingFailureException last = null;
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            try {
                return action.get();
            } catch (OptimisticLockingFailureException e) {
                last = e;
            }
        }
        throw last != null ? last : new OptimisticLockingFailureException("PO qty update failed");
    }

    public record PostedBillLineQty(UUID purchaseOrderLineId, BigDecimal qty) {}
}
