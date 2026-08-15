package com.jalaldeveloper.accountingsystem.purchase.service.domain;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.StockMovePurchaseQueryPort;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductRepository;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.PurchaseOrder;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.PurchaseOrderLine;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository.PurchaseOrderRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies PO qty updates in a fresh transaction and retries if another writer
 * (Rabbit receive-sync vs bill post) already incremented {@code row_version}.
 */
@Component
public class PurchaseOrderQtyWriter {

    private static final int MAX_ATTEMPTS = 8;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockMovePurchaseQueryPort stockMovePurchaseQueryPort;
    private final ProductRepository productRepository;
    private final TransactionTemplate requiresNew;

    public PurchaseOrderQtyWriter(PurchaseOrderRepository purchaseOrderRepository,
                                  StockMovePurchaseQueryPort stockMovePurchaseQueryPort,
                                  ProductRepository productRepository,
                                  PlatformTransactionManager transactionManager) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.stockMovePurchaseQueryPort = stockMovePurchaseQueryPort;
        this.productRepository = productRepository;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public PurchaseOrder updateQtyReceived(UUID purchaseOrderId) {
        return retry(() -> doUpdateQtyReceived(purchaseOrderId));
    }

    public void applyPostedBillQuantities(UUID purchaseOrderId,
                                          boolean creditNote,
                                          List<PostedBillLineQty> lines) {
        retry(() -> requiresNew.execute(status -> {
            doApplyPostedBillQuantities(purchaseOrderId, creditNote, lines);
            return null;
        }));
    }

    private PurchaseOrder doUpdateQtyReceived(UUID purchaseOrderId) {
        PurchaseOrder o = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId).orElse(null);
        if (o == null) {
            return null;
        }
        Instant now = Instant.now();
        for (PurchaseOrderLine line : o.getLines()) {
            BigDecimal sum = stockMovePurchaseQueryPort.sumPickedQuantityForPurchaseOrderLine(line.getId());
            line.setQtyReceived(sum.setScale(4, RoundingMode.HALF_UP));
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
        return purchaseOrderRepository.save(o);
    }

    private void doApplyPostedBillQuantities(UUID purchaseOrderId,
                                             boolean creditNote,
                                             List<PostedBillLineQty> lines) {
        PurchaseOrder po = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId).orElse(null);
        if (po == null) {
            return;
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
        purchaseOrderRepository.save(po);
    }

    private <T> T retry(java.util.function.Supplier<T> action) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException ex) {
                if (!isOptimisticLock(ex) || attempt == MAX_ATTEMPTS - 1) {
                    throw ex;
                }
                last = ex;
                try {
                    Thread.sleep(30L * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw last != null ? last : new IllegalStateException("PO qty update failed");
    }

    private static boolean isOptimisticLock(Throwable ex) {
        while (ex != null) {
            if (ex instanceof OptimisticLockingFailureException) {
                return true;
            }
            String name = ex.getClass().getName();
            if (name.contains("OptimisticLock") || name.contains("StaleState") || name.contains("StaleObject")) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }

    public record PostedBillLineQty(UUID purchaseOrderLineId, BigDecimal qty) {}
}
