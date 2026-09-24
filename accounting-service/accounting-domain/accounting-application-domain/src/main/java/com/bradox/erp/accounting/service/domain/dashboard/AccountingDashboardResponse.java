package com.bradox.erp.accounting.service.domain.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountingDashboardResponse {

    private LocalDate from;
    private LocalDate to;
    private String granularity;
    private KpiMetric invoices;
    private KpiMetric bills;
    private KpiMetric income;
    private KpiMetric spend;
    private List<SeriesPoint> series = new ArrayList<>();
    private List<RankedDocumentRow> topInvoices = new ArrayList<>();
    private List<RankedDocumentRow> topBills = new ArrayList<>();

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public String getGranularity() { return granularity; }
    public void setGranularity(String granularity) { this.granularity = granularity; }
    public KpiMetric getInvoices() { return invoices; }
    public void setInvoices(KpiMetric invoices) { this.invoices = invoices; }
    public KpiMetric getBills() { return bills; }
    public void setBills(KpiMetric bills) { this.bills = bills; }
    public KpiMetric getIncome() { return income; }
    public void setIncome(KpiMetric income) { this.income = income; }
    public KpiMetric getSpend() { return spend; }
    public void setSpend(KpiMetric spend) { this.spend = spend; }
    public List<SeriesPoint> getSeries() { return series; }
    public void setSeries(List<SeriesPoint> series) { this.series = series; }
    public List<RankedDocumentRow> getTopInvoices() { return topInvoices; }
    public void setTopInvoices(List<RankedDocumentRow> topInvoices) { this.topInvoices = topInvoices; }
    public List<RankedDocumentRow> getTopBills() { return topBills; }
    public void setTopBills(List<RankedDocumentRow> topBills) { this.topBills = topBills; }

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
        private BigDecimal spend;
        private long orderCount;

        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public LocalDate getPeriodStart() { return periodStart; }
        public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public BigDecimal getSpend() { return spend; }
        public void setSpend(BigDecimal spend) { this.spend = spend; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
    }

    public static class RankedDocumentRow {
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
}
