package com.bradox.erp.sales.service.domain;

import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse.ProductProfitRow;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse.PendingDelivery;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse.Pipeline;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse.PipelineStage;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse.WaitingOrder;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse.ProfitSlice;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse.ProfitTotals;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort.ConfirmedProductFact;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort.CostFact;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort.InvoicedProductFact;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort.LedgerProfitTotals;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort.PendingDeliveryLine;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort.PipelineFact;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SalesProductProfitService {

    private final SalesProductProfitQueryPort queryPort;

    public SalesProductProfitService(SalesProductProfitQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public SalesProductProfitResponse getProductProfit(UUID companyId, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must be on or after from");
        }

        List<ConfirmedProductFact> confirmed = queryPort.confirmedProductFacts(companyId, from, to);
        List<InvoicedProductFact> invoiced = queryPort.invoicedProductFacts(companyId, from, to);
        List<CostFact> costs = queryPort.costOfSalesByProduct(companyId, from, to);
        List<PendingDeliveryLine> pendingLines = queryPort.pendingDeliveryLines(companyId, from, to);
        Map<UUID, BigDecimal> pendingRevenueByProduct = new HashMap<>();
        for (PendingDeliveryLine line : pendingLines) {
            if (line.productId() != null) {
                pendingRevenueByProduct.merge(line.productId(), nz(line.revenue()), BigDecimal::add);
            }
        }

        Map<UUID, ProductProfitRow> byProduct = new LinkedHashMap<>();
        for (ConfirmedProductFact fact : confirmed) {
            ProductProfitRow row = byProduct.computeIfAbsent(fact.productId(), id -> newRow(id, fact.productName()));
            if (row.getProductName() == null && fact.productName() != null) {
                row.setProductName(fact.productName());
            }
            row.setOrderCount(fact.orderCount());
            row.setQtyOrdered(scaleQty(fact.qtyOrdered()));
            row.getEstimated().setRevenue(scale(fact.revenue()));
        }
        // Realized = delivered and invoiced: invoiced net sales less what is still waiting for delivery
        // (its cost of goods sold is not booked yet), against the cost of goods sold posted.
        for (InvoicedProductFact fact : invoiced) {
            ProductProfitRow row = byProduct.computeIfAbsent(fact.productId(), id -> newRow(id, fact.productName()));
            if (row.getProductName() == null && fact.productName() != null) {
                row.setProductName(fact.productName());
            }
            BigDecimal waiting = pendingRevenueByProduct.getOrDefault(fact.productId(), BigDecimal.ZERO);
            row.setQtyInvoiced(scaleQty(fact.qtyInvoiced()));
            row.getRealized().setRevenue(scale(nz(fact.revenue()).subtract(waiting).max(BigDecimal.ZERO)));
        }
        for (CostFact fact : costs) {
            ProductProfitRow row = byProduct.computeIfAbsent(fact.key(), id -> newRow(id, null));
            row.getRealized().setCost(scale(fact.cost()));
        }

        Map<UUID, BigDecimal> unitCosts = queryPort.currentValuationUnitCosts(companyId,
                byProduct.keySet().stream().filter(java.util.Objects::nonNull).toList());
        for (ProductProfitRow row : byProduct.values()) {
            BigDecimal unitCost = unitCosts.getOrDefault(row.getProductId(), BigDecimal.ZERO);
            BigDecimal estimatedCost = row.getQtyOrdered().multiply(unitCost).setScale(4, RoundingMode.HALF_UP);
            row.getEstimated().setCost(estimatedCost);
            fillProfit(row.getEstimated());
            fillProfit(row.getRealized());
        }

        List<ProductProfitRow> products = new ArrayList<>(byProduct.values());
        products.sort(Comparator
                .comparing((ProductProfitRow r) -> nz(r.getRealized().getProfit())).reversed()
                .thenComparing(r -> nz(r.getEstimated().getProfit()), Comparator.reverseOrder())
                .thenComparing(r -> r.getProductName() != null ? r.getProductName() : "", String.CASE_INSENSITIVE_ORDER));

        // Estimated totals cover every product. The Profit & Loss figures come straight from the ledger;
        // realized is those minus the sales still waiting for delivery, so
        // P&L net sales = realized sales + awaiting delivery, and P&L gross profit = realized profit
        // + awaiting-delivery sales (whose cost is not booked yet).
        ProfitTotals totals = new ProfitTotals();
        totals.setEstimated(sumSlice(products.stream().map(ProductProfitRow::getEstimated).toList()));
        LedgerProfitTotals ledger = queryPort.ledgerProfitTotals(companyId, from, to);
        ProfitSlice pnl = new ProfitSlice();
        pnl.setRevenue(scale(ledger.netSales()));
        pnl.setCost(scale(ledger.costOfRevenue()));
        fillProfit(pnl);
        totals.setProfitAndLoss(pnl);
        PendingDelivery pending = pendingDelivery(companyId, pendingLines);
        ProfitSlice realizedTotals = new ProfitSlice();
        realizedTotals.setRevenue(scale(nz(ledger.netSales()).subtract(nz(pending.getRevenue())).max(BigDecimal.ZERO)));
        realizedTotals.setCost(scale(ledger.costOfRevenue()));
        fillProfit(realizedTotals);
        totals.setRealized(realizedTotals);
        totals.setPendingDelivery(pending);
        Pipeline pipeline = new Pipeline();
        pipeline.setQuotations(stage(queryPort.quotationPipeline(companyId, from, to)));
        pipeline.setConfirmedNotInvoiced(stage(queryPort.confirmedNotInvoiced(companyId, from, to)));
        totals.setPipeline(pipeline);

        SalesProductProfitResponse response = new SalesProductProfitResponse();
        response.setFrom(from);
        response.setTo(to);
        response.setProducts(products);
        response.setTotals(totals);
        return response;
    }

    private static PipelineStage stage(PipelineFact fact) {
        PipelineStage stage = new PipelineStage();
        stage.setOrders(fact != null ? fact.orders() : 0);
        stage.setAmount(scale(fact != null ? fact.amount() : null));
        return stage;
    }

    /** Invoiced-but-undelivered sales, what their cost will be once they ship, and who is waiting longest. */
    private PendingDelivery pendingDelivery(UUID companyId, List<PendingDeliveryLine> lines) {
        Map<UUID, BigDecimal> unitCosts = queryPort.currentValuationUnitCosts(companyId,
                lines.stream().map(PendingDeliveryLine::productId).filter(java.util.Objects::nonNull).distinct().toList());
        BigDecimal units = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        LocalDate oldest = null;
        Map<UUID, WaitingOrder> byOrder = new LinkedHashMap<>();
        for (PendingDeliveryLine line : lines) {
            units = units.add(nz(line.units()));
            revenue = revenue.add(nz(line.revenue()));
            cost = cost.add(nz(line.units()).multiply(unitCosts.getOrDefault(line.productId(), BigDecimal.ZERO)));
            if (line.firstInvoiceDate() != null && (oldest == null || line.firstInvoiceDate().isBefore(oldest))) {
                oldest = line.firstInvoiceDate();
            }
            WaitingOrder order = byOrder.computeIfAbsent(line.orderId(), id -> {
                WaitingOrder w = new WaitingOrder();
                w.setOrderId(id);
                w.setOrderName(line.orderName());
                w.setCustomerName(line.customerName());
                w.setInvoiceDate(line.firstInvoiceDate());
                return w;
            });
            order.setUnits(order.getUnits().add(nz(line.units())));
            order.setRevenue(order.getRevenue().add(nz(line.revenue())));
            if (line.firstInvoiceDate() != null
                    && (order.getInvoiceDate() == null || line.firstInvoiceDate().isBefore(order.getInvoiceDate()))) {
                order.setInvoiceDate(line.firstInvoiceDate());
            }
        }
        List<WaitingOrder> waiting = new ArrayList<>(byOrder.values());
        waiting.sort(Comparator
                .comparing(WaitingOrder::getInvoiceDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(w -> nz(w.getRevenue()), Comparator.reverseOrder()));
        waiting.forEach(w -> {
            w.setUnits(scaleQty(w.getUnits()));
            w.setRevenue(scale(w.getRevenue()));
        });
        PendingDelivery pending = new PendingDelivery();
        pending.setUnits(scaleQty(units));
        pending.setRevenue(scale(revenue));
        pending.setCost(scale(cost));
        pending.setOrderCount(byOrder.size());
        pending.setOldestInvoiceDate(oldest);
        pending.setOrders(waiting.size() > 6 ? new ArrayList<>(waiting.subList(0, 6)) : waiting);
        return pending;
    }

    private static ProductProfitRow newRow(UUID productId, String productName) {
        ProductProfitRow row = new ProductProfitRow();
        row.setProductId(productId);
        row.setProductName(productName);
        row.setOrderCount(0);
        row.setQtyOrdered(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        row.setQtyInvoiced(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        row.setEstimated(emptySlice());
        row.setRealized(emptySlice());
        return row;
    }

    private static ProfitSlice emptySlice() {
        ProfitSlice slice = new ProfitSlice();
        slice.setRevenue(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        slice.setCost(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        slice.setProfit(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        slice.setMarginPercent(null);
        return slice;
    }

    private static void fillProfit(ProfitSlice slice) {
        BigDecimal revenue = nz(slice.getRevenue());
        BigDecimal cost = nz(slice.getCost());
        BigDecimal profit = revenue.subtract(cost).setScale(4, RoundingMode.HALF_UP);
        slice.setRevenue(scale(revenue));
        slice.setCost(scale(cost));
        slice.setProfit(profit);
        if (revenue.signum() == 0) {
            slice.setMarginPercent(null);
        } else {
            slice.setMarginPercent(profit.multiply(BigDecimal.valueOf(100))
                    .divide(revenue, 1, RoundingMode.HALF_UP));
        }
    }

    private static ProfitSlice sumSlice(List<ProfitSlice> slices) {
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (ProfitSlice slice : slices) {
            revenue = revenue.add(nz(slice.getRevenue()));
            cost = cost.add(nz(slice.getCost()));
        }
        ProfitSlice total = new ProfitSlice();
        total.setRevenue(scale(revenue));
        total.setCost(scale(cost));
        fillProfit(total);
        return total;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static BigDecimal scale(BigDecimal value) {
        return nz(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal scaleQty(BigDecimal value) {
        return nz(value).setScale(4, RoundingMode.HALF_UP);
    }
}
