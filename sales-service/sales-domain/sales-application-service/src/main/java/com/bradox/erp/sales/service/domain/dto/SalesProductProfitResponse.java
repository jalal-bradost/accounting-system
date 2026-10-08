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

    /**
     * Realized sales whose cost is not booked yet: invoiced but not delivered. COGS posts on
     * delivery, so realized profit is higher than it will be once these ship.
     */
    public static class PendingDelivery {
        private BigDecimal units = BigDecimal.ZERO;
        private BigDecimal revenue = BigDecimal.ZERO;
        /** Estimated at current stock cost. */
        private BigDecimal cost = BigDecimal.ZERO;
        private long orderCount;
        /** Oldest invoice among them in the period, i.e. how long the longest has been waiting. */
        private LocalDate oldestInvoiceDate;
        /** The orders waiting longest first (capped), for the "ready to ship" queue. */
        private List<WaitingOrder> orders = new ArrayList<>();

        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
        public LocalDate getOldestInvoiceDate() { return oldestInvoiceDate; }
        public void setOldestInvoiceDate(LocalDate oldestInvoiceDate) { this.oldestInvoiceDate = oldestInvoiceDate; }
        public List<WaitingOrder> getOrders() { return orders; }
        public void setOrders(List<WaitingOrder> orders) { this.orders = orders; }

        public BigDecimal getUnits() { return units; }
        public void setUnits(BigDecimal units) { this.units = units; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }
    }

    public static class WaitingOrder {
        private UUID orderId;
        private String orderName;
        private String customerName;
        private BigDecimal units = BigDecimal.ZERO;
        private BigDecimal revenue = BigDecimal.ZERO;
        private LocalDate invoiceDate;

        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        public String getOrderName() { return orderName; }
        public void setOrderName(String orderName) { this.orderName = orderName; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public BigDecimal getUnits() { return units; }
        public void setUnits(BigDecimal units) { this.units = units; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public LocalDate getInvoiceDate() { return invoiceDate; }
        public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    }

    public static class PipelineStage {
        private long orders;
        private BigDecimal amount = BigDecimal.ZERO;

        public long getOrders() { return orders; }
        public void setOrders(long orders) { this.orders = orders; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /** Where sales sit before they become realized: quotes, confirmed-not-invoiced (awaiting delivery is separate). */
    public static class Pipeline {
        private PipelineStage quotations = new PipelineStage();
        private PipelineStage confirmedNotInvoiced = new PipelineStage();

        public PipelineStage getQuotations() { return quotations; }
        public void setQuotations(PipelineStage quotations) { this.quotations = quotations; }
        public PipelineStage getConfirmedNotInvoiced() { return confirmedNotInvoiced; }
        public void setConfirmedNotInvoiced(PipelineStage confirmedNotInvoiced) { this.confirmedNotInvoiced = confirmedNotInvoiced; }
    }

    public static class ProfitTotals {
        /** The Profit &amp; Loss as booked: net sales and cost of goods sold from the ledger. */
        private ProfitSlice profitAndLoss = new ProfitSlice();
        private Pipeline pipeline = new Pipeline();

        public ProfitSlice getProfitAndLoss() { return profitAndLoss; }
        public void setProfitAndLoss(ProfitSlice profitAndLoss) { this.profitAndLoss = profitAndLoss; }
        public Pipeline getPipeline() { return pipeline; }
        public void setPipeline(Pipeline pipeline) { this.pipeline = pipeline; }

        private ProfitSlice estimated = new ProfitSlice();
        private ProfitSlice realized = new ProfitSlice();
        private PendingDelivery pendingDelivery = new PendingDelivery();

        public PendingDelivery getPendingDelivery() { return pendingDelivery; }
        public void setPendingDelivery(PendingDelivery pendingDelivery) { this.pendingDelivery = pendingDelivery; }

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
        private BigDecimal qtyInvoiced = BigDecimal.ZERO;
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
        public BigDecimal getQtyInvoiced() { return qtyInvoiced; }
        public void setQtyInvoiced(BigDecimal qtyInvoiced) { this.qtyInvoiced = qtyInvoiced; }
        public ProfitSlice getEstimated() { return estimated; }
        public void setEstimated(ProfitSlice estimated) { this.estimated = estimated; }
        public ProfitSlice getRealized() { return realized; }
        public void setRealized(ProfitSlice realized) { this.realized = realized; }
    }
}
