package com.bradox.erp.sales.service.domain;

import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse.ProductProfitRow;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse.ProfitSlice;
import com.bradox.erp.sales.service.domain.dto.SalesProductProfitResponse.ProfitTotals;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort.ConfirmedProductFact;
import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort.DeliveredProductFact;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
        List<DeliveredProductFact> delivered = queryPort.deliveredProductFacts(companyId, from, to);

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
        for (DeliveredProductFact fact : delivered) {
            ProductProfitRow row = byProduct.computeIfAbsent(fact.productId(), id -> newRow(id, fact.productName()));
            if (row.getProductName() == null && fact.productName() != null) {
                row.setProductName(fact.productName());
            }
            row.setQtyDelivered(scaleQty(fact.qtyDelivered()));
            row.getRealized().setRevenue(scale(fact.revenue()));
            row.getRealized().setCost(scale(fact.cost().max(BigDecimal.ZERO)));
        }

        Map<UUID, BigDecimal> unitCosts = queryPort.currentValuationUnitCosts(companyId, byProduct.keySet());
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

        // Totals and rows cover every product so the cards match the ledger; the page ranks per mode.
        ProfitTotals totals = new ProfitTotals();
        totals.setEstimated(sumSlice(products.stream().map(ProductProfitRow::getEstimated).toList()));
        totals.setRealized(sumSlice(products.stream().map(ProductProfitRow::getRealized).toList()));

        SalesProductProfitResponse response = new SalesProductProfitResponse();
        response.setFrom(from);
        response.setTo(to);
        response.setProducts(products);
        response.setTotals(totals);
        return response;
    }

    private static ProductProfitRow newRow(UUID productId, String productName) {
        ProductProfitRow row = new ProductProfitRow();
        row.setProductId(productId);
        row.setProductName(productName);
        row.setOrderCount(0);
        row.setQtyOrdered(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        row.setQtyDelivered(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
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
