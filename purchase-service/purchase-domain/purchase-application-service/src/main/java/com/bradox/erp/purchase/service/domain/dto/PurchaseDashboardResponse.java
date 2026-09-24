package com.bradox.erp.purchase.service.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PurchaseDashboardResponse {

    private LocalDate from;
    private LocalDate to;
    private String granularity;
    private KpiMetric rfqs;
    private KpiMetric orders;
    private KpiMetric spend;
    private KpiMetric averageOrder;
    private List<SeriesPoint> series = new ArrayList<>();
    private List<RankedOrderRow> topRfqs = new ArrayList<>();
    private List<RankedOrderRow> topOrders = new ArrayList<>();
    private List<RankedProductRow> topProducts = new ArrayList<>();
    private List<CategoryNode> topCategories = new ArrayList<>();

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public String getGranularity() { return granularity; }
    public void setGranularity(String granularity) { this.granularity = granularity; }
    public KpiMetric getRfqs() { return rfqs; }
    public void setRfqs(KpiMetric rfqs) { this.rfqs = rfqs; }
    public KpiMetric getOrders() { return orders; }
    public void setOrders(KpiMetric orders) { this.orders = orders; }
    public KpiMetric getSpend() { return spend; }
    public void setSpend(KpiMetric spend) { this.spend = spend; }
    public KpiMetric getAverageOrder() { return averageOrder; }
    public void setAverageOrder(KpiMetric averageOrder) { this.averageOrder = averageOrder; }
    public List<SeriesPoint> getSeries() { return series; }
    public void setSeries(List<SeriesPoint> series) { this.series = series; }
    public List<RankedOrderRow> getTopRfqs() { return topRfqs; }
    public void setTopRfqs(List<RankedOrderRow> topRfqs) { this.topRfqs = topRfqs; }
    public List<RankedOrderRow> getTopOrders() { return topOrders; }
    public void setTopOrders(List<RankedOrderRow> topOrders) { this.topOrders = topOrders; }
    public List<RankedProductRow> getTopProducts() { return topProducts; }
    public void setTopProducts(List<RankedProductRow> topProducts) { this.topProducts = topProducts; }
    public List<CategoryNode> getTopCategories() { return topCategories; }
    public void setTopCategories(List<CategoryNode> topCategories) { this.topCategories = topCategories; }

    public static class KpiMetric {
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

    public static class SeriesPoint {
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

    public static class RankedOrderRow {
        private UUID id;
        private String name;
        private String customerName;
        private String partnerName;
        private BigDecimal revenue;
        private LocalDate orderDate;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getPartnerName() { return partnerName; }
        public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public LocalDate getOrderDate() { return orderDate; }
        public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    }

    public static class RankedProductRow {
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

    public static class CategoryNode {
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
}
