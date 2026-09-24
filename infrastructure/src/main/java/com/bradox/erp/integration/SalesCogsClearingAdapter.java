package com.bradox.erp.integration;

import com.bradox.erp.accounting.service.domain.ports.output.SalesCogsClearingPort;
import com.bradox.erp.accounting.service.domain.ports.output.SalesOrderCogsStatePort;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.Money;
import com.bradox.erp.inventory.domain.core.entity.Product;
import com.bradox.erp.inventory.domain.core.entity.ProductCategory;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.service.domain.ports.output.StockMoveSalesQueryPort;
import com.bradox.erp.inventory.service.domain.ports.output.accounting.JournalEntryPostingPort;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductCategoryRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reconciles Stock Output → COGS to {@code min(qtyDelivered, qtyInvoiced)} so COGS posts at
 * whichever of delivery or invoice happens second.
 */
@Component
public class SalesCogsClearingAdapter implements SalesCogsClearingPort {

    private final ObjectProvider<SalesOrderCogsStatePort> salesOrderCogsStatePortProvider;
    private final StockMoveSalesQueryPort stockMoveSalesQueryPort;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ObjectProvider<JournalEntryPostingPort> journalPostingProvider;

    public SalesCogsClearingAdapter(ObjectProvider<SalesOrderCogsStatePort> salesOrderCogsStatePortProvider,
                                    StockMoveSalesQueryPort stockMoveSalesQueryPort,
                                    ProductRepository productRepository,
                                    ProductCategoryRepository productCategoryRepository,
                                    ObjectProvider<JournalEntryPostingPort> journalPostingProvider) {
        this.salesOrderCogsStatePortProvider = salesOrderCogsStatePortProvider;
        this.stockMoveSalesQueryPort = stockMoveSalesQueryPort;
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.journalPostingProvider = journalPostingProvider;
    }

    @Override
    @Transactional
    public void reconcileForSalesOrder(UUID salesOrderId) {
        if (salesOrderId == null) {
            return;
        }
        SalesOrderCogsStatePort statePort = salesOrderCogsStatePortProvider.getIfAvailable();
        JournalEntryPostingPort posting = journalPostingProvider.getIfAvailable();
        if (statePort == null || posting == null) {
            return;
        }
        Optional<SalesOrderCogsStatePort.OrderState> orderOpt = statePort.findOrder(salesOrderId);
        if (orderOpt.isEmpty()) {
            return;
        }
        SalesOrderCogsStatePort.OrderState order = orderOpt.get();
        List<JournalEntryPostingPort.JournalLine> lines = new ArrayList<>();
        Map<UUID, BigDecimal> newCleared = new LinkedHashMap<>();

        for (SalesOrderCogsStatePort.LineState line : order.lines()) {
            BigDecimal delivered = nz(line.qtyDelivered());
            BigDecimal invoiced = nz(line.qtyInvoiced());
            BigDecimal cleared = nz(line.qtyCogsCleared());
            BigDecimal target = delivered.min(invoiced).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            BigDecimal delta = target.subtract(cleared).setScale(4, RoundingMode.HALF_UP);
            if (delta.signum() == 0) {
                continue;
            }
            Optional<StockMoveSalesQueryPort.NetDeliveredCost> costOpt =
                    stockMoveSalesQueryPort.grossDeliveredCostForSalesOrderLine(line.lineId());
            if (costOpt.isEmpty()) {
                // No valued delivery yet (or only services) — cannot clear.
                continue;
            }
            StockMoveSalesQueryPort.NetDeliveredCost cost = costOpt.get();
            if (cost.quantity().signum() <= 0 || cost.value().signum() <= 0) {
                continue;
            }
            Product product = productRepository.findById(new ProductId(cost.productId())).orElse(null);
            if (product == null || !product.isValued()) {
                continue;
            }
            ProductCategory category = product.getCategoryId() != null
                    ? productCategoryRepository.findById(product.getCategoryId()).orElse(null)
                    : null;
            UUID cogs = product.resolveCogsAccountId(category);
            UUID stockOutput = product.resolveStockOutputAccountId(category);
            if (cogs == null || stockOutput == null) {
                continue;
            }
            BigDecimal unitCost = cost.value().divide(cost.quantity(), 8, RoundingMode.HALF_UP);
            BigDecimal amount = unitCost.multiply(delta.abs()).setScale(4, RoundingMode.HALF_UP);
            if (amount.signum() <= 0) {
                continue;
            }
            Money money = new Money(amount);
            String label = "COGS " + (line.name() != null && !line.name().isBlank()
                    ? line.name()
                    : product.getSku());
            if (delta.signum() > 0) {
                lines.add(new JournalEntryPostingPort.JournalLine(cogs, label, money, Money.ZERO));
                lines.add(new JournalEntryPostingPort.JournalLine(stockOutput, label, Money.ZERO, money));
            } else {
                lines.add(new JournalEntryPostingPort.JournalLine(stockOutput, label, money, Money.ZERO));
                lines.add(new JournalEntryPostingPort.JournalLine(cogs, label, Money.ZERO, money));
            }
            newCleared.put(line.lineId(), target);
        }

        if (!lines.isEmpty()) {
            posting.postValuationEntry(
                    new CompanyId(order.companyId()),
                    LocalDate.now(),
                    "COGS-CLR-" + salesOrderId,
                    null,
                    lines);
            statePort.updateQtyCogsCleared(salesOrderId, newCleared);
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
