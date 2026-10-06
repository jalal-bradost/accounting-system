package com.bradox.erp.sales.service.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SalesProductProfitResponse {

    private LocalDate from;
    private LocalDate to;
    private List<ProductProfitRow> products = new ArrayList<>();
    private ProfitTotals totals = new ProfitTotals();

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public List<ProductProfitRow> getProducts() { return products; }
    public void setProducts(List<ProductProfitRow> products) { this.products = products; }
    public ProfitTotals getTotals() { return totals; }
    public void setTotals(ProfitTotals totals) { this.totals = totals; }

    public static class ProfitSlice {
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;
        private BigDecimal marginPercent;

        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }
        public BigDecimal getProfit() { return profit; }
        public void setProfit(BigDecimal profit) { this.profit = profit; }
        public BigDecimal getMarginPercent() { return marginPercent; }
        public void setMarginPercent(BigDecimal marginPercent) { this.marginPercent = marginPercent; }
    }

    public static class ProfitTotals {
        private ProfitSlice estimated = new ProfitSlice();
        private ProfitSlice realized = new ProfitSlice();

        public ProfitSlice getEstimated() { return estimated; }
        public void setEstimated(ProfitSlice estimated) { this.estimated = estimated; }
        public ProfitSlice getRealized() { return realized; }
        public void setRealized(ProfitSlice realized) { this.realized = realized; }
    }

    public static class ProductProfitRow {
        private UUID productId;
        private String productName;
        private long orderCount;
        private BigDecimal qtyOrdered = BigDecimal.ZERO;
        private BigDecimal qtyDelivered = BigDecimal.ZERO;
        private ProfitSlice estimated = new ProfitSlice();
        private ProfitSlice realized = new ProfitSlice();

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
        public BigDecimal getQtyOrdered() { return qtyOrdered; }
        public void setQtyOrdered(BigDecimal qtyOrdered) { this.qtyOrdered = qtyOrdered; }
        public BigDecimal getQtyDelivered() { return qtyDelivered; }
        public void setQtyDelivered(BigDecimal qtyDelivered) { this.qtyDelivered = qtyDelivered; }
        public ProfitSlice getEstimated() { return estimated; }
        public void setEstimated(ProfitSlice estimated) { this.estimated = estimated; }
        public ProfitSlice getRealized() { return realized; }
        public void setRealized(ProfitSlice realized) { this.realized = realized; }
    }
}
