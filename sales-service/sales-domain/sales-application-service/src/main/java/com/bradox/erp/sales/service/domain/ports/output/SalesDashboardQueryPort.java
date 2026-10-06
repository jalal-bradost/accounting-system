package com.bradox.erp.sales.service.domain.ports.output;

import com.bradox.erp.sales.service.domain.dto.SalesDashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalesDashboardQueryPort {

    long countQuotations(UUID companyId, LocalDate from, LocalDate to);

    /** Confirmed order count with remaining net revenue (ordered qty net of returns through {@code to}). */
    long countConfirmedOrders(UUID companyId, LocalDate from, LocalDate to);

    /** Confirmed sales revenue net of customer returns through {@code to}. */
    BigDecimal sumConfirmedRevenue(UUID companyId, LocalDate from, LocalDate to);

    /** Confirmed order revenue facts (amounts net of customer returns through {@code to}). */
    List<ConfirmedOrderFact> listConfirmedOrderFacts(UUID companyId, LocalDate from, LocalDate to);

    List<SalesDashboardResponse.SalesRankedOrderRow> topQuotations(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<SalesDashboardResponse.SalesRankedOrderRow> topConfirmedOrders(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<SalesDashboardResponse.SalesRankedProductRow> topProducts(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<SalesDashboardResponse.SalesCategoryNode> topCategories(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<SalesDashboardResponse.SalesNamedAmount> channelSplit(UUID companyId, LocalDate from, LocalDate to);

    List<SalesDashboardResponse.SalesNamedAmount> paymentMethodSplit(UUID companyId, LocalDate from, LocalDate to);

    record ConfirmedOrderFact(LocalDate confirmedDate, BigDecimal companyAmount) {}
}
