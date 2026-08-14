package com.jalaldeveloper.accountingsystem.sales.service.domain;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.StockMoveSalesQueryPort;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductRepository;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalInvoicePolicy;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderDeliveryStatus;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderInvoiceStatus;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.SalesOrder;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.SalesOrderLine;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.repository.SalesOrderRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies SO qty updates in a fresh transaction and retries if another writer
 * (Rabbit delivery-sync vs invoice post) already incremented {@code row_version}.
 */
@Component
public class SalesOrderQtyWriter {

    private static final int MAX_ATTEMPTS = 8;

    private final SalesOrderRepository salesOrderRepository;
    private final StockMoveSalesQueryPort stockMoveSalesQueryPort;
    private final ProductRepository productRepository;
    private final TransactionTemplate requiresNew;

    public SalesOrderQtyWriter(SalesOrderRepository salesOrderRepository,
                               StockMoveSalesQueryPort stockMoveSalesQueryPort,
                               ProductRepository productRepository,
                               PlatformTransactionManager transactionManager) {
        this.salesOrderRepository = salesOrderRepository;
        this.stockMoveSalesQueryPort = stockMoveSalesQueryPort;
        this.productRepository = productRepository;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public SalesOrder updateQtyDelivered(UUID salesOrderId) {
        return retry(() -> requiresNew.execute(status -> doUpdateQtyDelivered(salesOrderId)));
    }

    /** Same as {@link #updateQtyDelivered} but joins the caller transaction (POS checkout). */
    public SalesOrder updateQtyDeliveredJoiningCurrentTransaction(UUID salesOrderId) {
        return doUpdateQtyDelivered(salesOrderId);
    }

    public void applyPostedInvoiceQuantities(UUID salesOrderId, Map<UUID, BigDecimal> invoicedQtyBySalesLineId) {
        retry(() -> requiresNew.execute(status -> {
            doApplyPostedInvoiceQuantities(salesOrderId, invoicedQtyBySalesLineId);
            return null;
        }));
    }

    private SalesOrder doUpdateQtyDelivered(UUID salesOrderId) {
        SalesOrder o = salesOrderRepository.findByIdForUpdate(salesOrderId).orElse(null);
        if (o == null) {
            return null;
        }
        Instant now = Instant.now();
        for (SalesOrderLine line : o.getLines()) {
            BigDecimal sum = stockMoveSalesQueryPort.sumPickedQuantityForSalesOrderLine(line.getId());
            line.setQtyDelivered(sum.setScale(4, RoundingMode.HALF_UP));
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
        return salesOrderRepository.save(o);
    }

    private void doApplyPostedInvoiceQuantities(UUID salesOrderId, Map<UUID, BigDecimal> invoicedQtyBySalesLineId) {
        SalesOrder o = salesOrderRepository.findByIdForUpdate(salesOrderId).orElse(null);
        if (o == null) {
            return;
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
        for (SalesOrderLine l : o.getLines()) {
            Product p = productRepository.findById(new ProductId(l.getProductId())).orElse(null);
            SalInvoicePolicy pol = l.getInvoicePolicy() != null ? l.getInvoicePolicy()
                    : (p != null && p.getProductType() == ProductType.SERVICE
                    ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED);
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
        salesOrderRepository.save(o);
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
        for (SalesOrderLine l : o.getLines()) {
            Product p = productRepository.findById(new ProductId(l.getProductId())).orElse(null);
            SalInvoicePolicy pol = l.getInvoicePolicy() != null ? l.getInvoicePolicy()
                    : (p != null && p.getProductType() == ProductType.SERVICE
                    ? SalInvoicePolicy.ORDERED : SalInvoicePolicy.DELIVERED);
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
