package com.bradox.erp.sales.service.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SalesDashboardResponse {

    private LocalDate from;
    private LocalDate to;
    private String granularity;
    private SalesKpiMetric quotations;
    private SalesKpiMetric orders;
    private SalesKpiMetric revenue;
    private SalesKpiMetric averageOrder;
    private List<SalesSeriesPoint> series = new ArrayList<>();
    private List<SalesRankedOrderRow> topQuotations = new ArrayList<>();
    private List<SalesRankedOrderRow> topOrders = new ArrayList<>();
    private List<SalesRankedProductRow> topProducts = new ArrayList<>();
    private List<SalesCategoryNode> topCategories = new ArrayList<>();
    private List<SalesNamedAmount> channels = new ArrayList<>();
    private List<SalesNamedAmount> paymentMethods = new ArrayList<>();

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public String getGranularity() { return granularity; }
    public void setGranularity(String granularity) { this.granularity = granularity; }
    public SalesKpiMetric getQuotations() { return quotations; }
    public void setQuotations(SalesKpiMetric quotations) { this.quotations = quotations; }
    public SalesKpiMetric getOrders() { return orders; }
    public void setOrders(SalesKpiMetric orders) { this.orders = orders; }
    public SalesKpiMetric getRevenue() { return revenue; }
    public void setRevenue(SalesKpiMetric revenue) { this.revenue = revenue; }
    public SalesKpiMetric getAverageOrder() { return averageOrder; }
    public void setAverageOrder(SalesKpiMetric averageOrder) { this.averageOrder = averageOrder; }
    public List<SalesSeriesPoint> getSeries() { return series; }
    public void setSeries(List<SalesSeriesPoint> series) { this.series = series; }
    public List<SalesRankedOrderRow> getTopQuotations() { return topQuotations; }
    public void setTopQuotations(List<SalesRankedOrderRow> topQuotations) { this.topQuotations = topQuotations; }
    public List<SalesRankedOrderRow> getTopOrders() { return topOrders; }
    public void setTopOrders(List<SalesRankedOrderRow> topOrders) { this.topOrders = topOrders; }
    public List<SalesRankedProductRow> getTopProducts() { return topProducts; }
    public void setTopProducts(List<SalesRankedProductRow> topProducts) { this.topProducts = topProducts; }
    public List<SalesCategoryNode> getTopCategories() { return topCategories; }
    public void setTopCategories(List<SalesCategoryNode> topCategories) { this.topCategories = topCategories; }
    public List<SalesNamedAmount> getChannels() { return channels; }
    public void setChannels(List<SalesNamedAmount> channels) { this.channels = channels; }
    public List<SalesNamedAmount> getPaymentMethods() { return paymentMethods; }
    public void setPaymentMethods(List<SalesNamedAmount> paymentMethods) { this.paymentMethods = paymentMethods; }

    public static class SalesKpiMetric {
        private BigDecimal value;
        private BigDecimal previousValue;
        private BigDecimal changePercent;

        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public BigDecimal getPreviousValue() { return previousValue; }
        public void setPreviousValue(BigDecimal previousValue) { this.previousValue = previousValue; }
        public BigDecimal getChangePercent() { return changePercent; }
        public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }
    }

    public static class SalesSeriesPoint {
        private String period;
        private LocalDate periodStart;
        private BigDecimal revenue;
        private long orderCount;

        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public LocalDate getPeriodStart() { return periodStart; }
        public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
    }

    public static class SalesRankedOrderRow {
        private UUID id;
        private String name;
        private String customerName;
        private BigDecimal revenue;
        private LocalDate orderDate;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public LocalDate getOrderDate() { return orderDate; }
        public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    }

    public static class SalesRankedProductRow {
        private UUID productId;
        private String productName;
        private long orderCount;
        private BigDecimal revenue;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    }

    public static class SalesCategoryNode {
        private UUID categoryId;
        private String categoryName;
        private BigDecimal revenue;
        private long orderCount;

        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
    }

    public static class SalesNamedAmount {
        private String key;
        private String label;
        private BigDecimal amount;
        private long count;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
}
