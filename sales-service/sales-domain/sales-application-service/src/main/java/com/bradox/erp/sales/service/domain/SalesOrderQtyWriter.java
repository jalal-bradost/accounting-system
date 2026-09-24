package com.bradox.erp.sales.service.domain;

import com.bradox.erp.inventory.domain.core.entity.Product;
import com.bradox.erp.inventory.domain.core.entity.ProductPackaging;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.domain.core.valueobject.ProductType;
import com.bradox.erp.inventory.service.domain.ports.input.UomApplicationService;
import com.bradox.erp.inventory.service.domain.ports.output.StockMoveSalesQueryPort;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductRepository;
import com.bradox.erp.platform.activity.RecordActivityLogger;
import com.bradox.erp.platform.settings.CompanyDocumentPolicyService;
import com.bradox.erp.sales.domain.core.SalInvoicePolicy;
import com.bradox.erp.sales.domain.core.SalesOrderDeliveryStatus;
import com.bradox.erp.sales.domain.core.SalesOrderInvoiceStatus;
import com.bradox.erp.sales.domain.core.entity.SalesOrder;
import com.bradox.erp.sales.domain.core.entity.SalesOrderLine;
import com.bradox.erp.sales.service.domain.ports.output.repository.SalesOrderRepository;
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
 * Applies SO qty updates and retries if another writer
 * (Rabbit delivery-sync vs invoice post) already incremented {@code row_version}.
 */
@Component
public class SalesOrderQtyWriter {

    private static final int MAX_ATTEMPTS = 8;

    private final SalesOrderRepository salesOrderRepository;
    private final StockMoveSalesQueryPort stockMoveSalesQueryPort;
    private final ProductRepository productRepository;
    private final UomApplicationService uomApplicationService;
    private final RecordActivityLogger activityLogger;
    private final CompanyDocumentPolicyService companyDocumentPolicyService;

    public SalesOrderQtyWriter(SalesOrderRepository salesOrderRepository,
                               StockMoveSalesQueryPort stockMoveSalesQueryPort,
                               ProductRepository productRepository,
                               UomApplicationService uomApplicationService,
                               RecordActivityLogger activityLogger,
                               CompanyDocumentPolicyService companyDocumentPolicyService) {
        this.salesOrderRepository = salesOrderRepository;
        this.stockMoveSalesQueryPort = stockMoveSalesQueryPort;
        this.productRepository = productRepository;
        this.uomApplicationService = uomApplicationService;
        this.activityLogger = activityLogger;
        this.companyDocumentPolicyService = companyDocumentPolicyService;
    }

    public SalesOrder updateQtyDelivered(UUID salesOrderId) {
        return retry(() -> doUpdateQtyDelivered(salesOrderId));
    }

    /** Same as {@link #updateQtyDelivered} but joins the caller transaction (POS / finalize-on-create). */
    public SalesOrder updateQtyDeliveredJoiningCurrentTransaction(UUID salesOrderId) {
        return doUpdateQtyDelivered(salesOrderId);
    }

    public void applyPostedInvoiceQuantities(UUID salesOrderId, Map<UUID, BigDecimal> invoicedQtyBySalesLineId) {
        // Join the caller transaction. Nested REQUIRES_NEW deadlocks when the outer TX already
        // holds a pessimistic lock on the same sales order (finalize-on-create).
        retry(() -> {
            doApplyPostedInvoiceQuantities(salesOrderId, invoicedQtyBySalesLineId);
            return null;
        });
    }

    private SalesOrder doUpdateQtyDelivered(UUID salesOrderId) {
        SalesOrder o = salesOrderRepository.findByIdForUpdate(salesOrderId).orElse(null);
        if (o == null) {
            return null;
        }
        Map<UUID, BigDecimal> before = new HashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            before.put(line.getId(), line.getQtyDelivered() != null ? line.getQtyDelivered() : BigDecimal.ZERO);
        }
        Instant now = Instant.now();
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal sumBase = stockMoveSalesQueryPort.sumPickedQuantityForSalesOrderLine(line.getId());
            line.setQtyDelivered(toDocumentQty(line, sumBase).setScale(4, RoundingMode.HALF_UP));
            line.setUpdatedAt(now);
        }
        boolean allDelivered = o.getLines().stream().allMatch(l -> {
            Optional<Product> p = productRepository.findById(new ProductId(l.getProductId()));
            if (p.isEmpty() || p.get().getProductType() == ProductType.SERVICE) {
                return true;
            }
            return l.getQtyDelivered().compareTo(l.getQtyOrdered()) >= 0;
        });
        if (allDelivered) {
            o.setDeliveryCompletedAt(now);
        }
        refreshOrderStatuses(o);
        o.setUpdatedAt(now);
        SalesOrder saved = salesOrderRepository.save(o);
        postDeliveredQtyTracking(saved, before);
        return saved;
    }

    private void postDeliveredQtyTracking(SalesOrder o, Map<UUID, BigDecimal> before) {
        List<String> blocks = new ArrayList<>();
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal oldQty = before.getOrDefault(line.getId(), BigDecimal.ZERO);
            BigDecimal newQty = line.getQtyDelivered() != null ? line.getQtyDelivered() : BigDecimal.ZERO;
            if (oldQty.compareTo(newQty) == 0) {
                continue;
            }
            String label = line.getName() != null && !line.getName().isBlank()
                    ? line.getName()
                    : line.getProductId().toString().substring(0, 8);
            Optional<Product> p = productRepository.findById(new ProductId(line.getProductId()));
            if (p.isPresent()) {
                label = "[" + p.get().getSku() + "] " + p.get().getName();
            }
            blocks.add("• " + label + ":\n  Delivered Quantity: "
                    + oldQty.stripTrailingZeros().toPlainString()
                    + " → "
                    + newQty.stripTrailingZeros().toPlainString());
        }
        if (blocks.isEmpty()) {
            return;
        }
        activityLogger.log(
                o.getCompanyId(),
                RecordActivityLogger.MODEL_SALES_ORDER,
                o.getId(),
                "The delivered quantity has been updated.\n" + String.join("\n", blocks));
    }

    private void doApplyPostedInvoiceQuantities(UUID salesOrderId, Map<UUID, BigDecimal> invoicedQtyBySalesLineId) {
        SalesOrder o = salesOrderRepository.findByIdForUpdate(salesOrderId).orElse(null);
        if (o == null) {
            return;
        }
        Map<UUID, BigDecimal> before = new LinkedHashMap<>();
        for (SalesOrderLine line : o.getLines()) {
            before.put(line.getId(), line.getQtyInvoiced() != null ? line.getQtyInvoiced() : BigDecimal.ZERO);
        }
        Instant now = Instant.now();
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal add = invoicedQtyBySalesLineId.get(line.getId());
            if (add != null && add.signum() != 0) {
                BigDecimal next = line.getQtyInvoiced().add(add).setScale(4, RoundingMode.HALF_UP);
                if (next.signum() < 0) {
                    next = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                }
                line.setQtyInvoiced(next);
                line.setUpdatedAt(now);
            }
        }
        boolean allInvoiced = true;
        boolean allowWithoutDelivery = companyDocumentPolicyService.allowInvoiceWithoutDelivery(o.getCompanyId());
        for (SalesOrderLine l : o.getLines()) {
            Product p = productRepository.findById(new ProductId(l.getProductId())).orElse(null);
            SalInvoicePolicy pol = effectiveInvoicePolicy(l, p, allowWithoutDelivery);
            BigDecimal target = pol == SalInvoicePolicy.ORDERED ? l.getQtyOrdered() : l.getQtyDelivered();
            if (l.getQtyInvoiced().compareTo(target) < 0) {
                allInvoiced = false;
                break;
            }
        }
        if (allInvoiced) {
            o.setInvoicingCompletedAt(now);
        }
        refreshOrderStatuses(o);
        o.setUpdatedAt(now);
        SalesOrder saved = salesOrderRepository.save(o);
        postInvoicedQtyTracking(saved, before);
    }

    private void postInvoicedQtyTracking(SalesOrder o, Map<UUID, BigDecimal> before) {
        List<String> blocks = new ArrayList<>();
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal oldQty = before.getOrDefault(line.getId(), BigDecimal.ZERO);
            BigDecimal newQty = line.getQtyInvoiced() != null ? line.getQtyInvoiced() : BigDecimal.ZERO;
            if (oldQty.compareTo(newQty) == 0) {
                continue;
            }
            String label = line.getName() != null && !line.getName().isBlank()
                    ? line.getName()
                    : line.getProductId().toString().substring(0, 8);
            Optional<Product> p = productRepository.findById(new ProductId(line.getProductId()));
            if (p.isPresent()) {
                label = "[" + p.get().getSku() + "] " + p.get().getName();
            }
            blocks.add("• " + label + ":\n  Invoiced Quantity: "
                    + oldQty.stripTrailingZeros().toPlainString()
                    + " → "
                    + newQty.stripTrailingZeros().toPlainString());
        }
        if (blocks.isEmpty()) {
            return;
        }
        activityLogger.log(
                o.getCompanyId(),
                RecordActivityLogger.MODEL_SALES_ORDER,
                o.getId(),
                "The invoiced quantity has been updated.\n" + String.join("\n", blocks));
    }

    private void refreshOrderStatuses(SalesOrder o) {
        boolean anyStockLine = o.getLines().stream().anyMatch(l -> {
            Optional<Product> p = productRepository.findById(new ProductId(l.getProductId()));
            return p.map(product -> product.getProductType() != ProductType.SERVICE).orElse(false);
        });
        if (!anyStockLine) {
            o.setDeliveryStatus(SalesOrderDeliveryStatus.NA);
        } else {
            boolean anyDelivered = o.getLines().stream().anyMatch(l -> l.getQtyDelivered().signum() > 0);
            boolean allDelivered = o.getLines().stream().allMatch(l -> {
                Optional<Product> p = productRepository.findById(new ProductId(l.getProductId()));
                if (p.isEmpty() || p.get().getProductType() == ProductType.SERVICE) {
                    return true;
                }
                return l.getQtyDelivered().compareTo(l.getQtyOrdered()) >= 0;
            });
            if (allDelivered) {
                o.setDeliveryStatus(SalesOrderDeliveryStatus.FULL);
            } else if (anyDelivered) {
                o.setDeliveryStatus(SalesOrderDeliveryStatus.PARTIAL);
            } else {
                o.setDeliveryStatus(SalesOrderDeliveryStatus.PENDING);
            }
        }

        boolean anyBillable = false;
        boolean allInvoiced = true;
        boolean allowWithoutDelivery = companyDocumentPolicyService.allowInvoiceWithoutDelivery(o.getCompanyId());
        for (SalesOrderLine l : o.getLines()) {
            Product p = productRepository.findById(new ProductId(l.getProductId())).orElse(null);
            SalInvoicePolicy pol = effectiveInvoicePolicy(l, p, allowWithoutDelivery);
            BigDecimal target = pol == SalInvoicePolicy.ORDERED ? l.getQtyOrdered() : l.getQtyDelivered();
            if (target.signum() > 0 && l.getQtyInvoiced().compareTo(target) < 0) {
                anyBillable = true;
            }
            if (l.getQtyInvoiced().compareTo(target) < 0) {
                allInvoiced = false;
            }
        }
        if (allInvoiced && !o.getLines().isEmpty()) {
            o.setInvoiceStatus(SalesOrderInvoiceStatus.FULL);
        } else if (anyBillable) {
            boolean anyInvoiced = o.getLines().stream().anyMatch(l -> l.getQtyInvoiced().signum() > 0);
            o.setInvoiceStatus(anyInvoiced ? SalesOrderInvoiceStatus.PARTIAL : SalesOrderInvoiceStatus.TO_INVOICE);
        } else {
            o.setInvoiceStatus(SalesOrderInvoiceStatus.NOTHING);
        }
    }

    private static SalInvoicePolicy effectiveInvoicePolicy(SalesOrderLine sol,
                                                           Product product,
                                                           boolean allowWithoutDelivery) {
        if (allowWithoutDelivery) {
            return SalInvoicePolicy.ORDERED;
        }
        if (sol.getInvoicePolicy() != null) {
            return sol.getInvoicePolicy();
        }
        return product != null && product.getProductType() == ProductType.SERVICE
                ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED;
    }

    /** Convert picked base-UOM quantity into the document line unit (packaging or line UOM). */
    private BigDecimal toDocumentQty(SalesOrderLine line, BigDecimal baseQty) {
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
        throw last != null ? last : new IllegalStateException("SO qty update failed");
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
}
