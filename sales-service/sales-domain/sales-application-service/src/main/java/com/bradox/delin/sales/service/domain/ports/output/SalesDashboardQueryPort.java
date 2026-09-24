package com.bradox.delin.sales.service.domain.ports.output;

import com.bradox.delin.sales.service.domain.dto.SalesDashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalesDashboardQueryPort {

    long countQuotations(UUID companyId, LocalDate from, LocalDate to);

    long countConfirmedOrders(UUID companyId, LocalDate from, LocalDate to);

    BigDecimal sumConfirmedRevenue(UUID companyId, LocalDate from, LocalDate to);

    List<ConfirmedOrderFact> listConfirmedOrderFacts(UUID companyId, LocalDate from, LocalDate to);

    List<SalesDashboardResponse.SalesRankedOrderRow> topQuotations(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<SalesDashboardResponse.SalesRankedOrderRow> topConfirmedOrders(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<SalesDashboardResponse.SalesRankedProductRow> topProducts(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<SalesDashboardResponse.SalesCategoryNode> topCategories(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<SalesDashboardResponse.SalesNamedAmount> channelSplit(UUID companyId, LocalDate from, LocalDate to);

    List<SalesDashboardResponse.SalesNamedAmount> paymentMethodSplit(UUID companyId, LocalDate from, LocalDate to);

    record ConfirmedOrderFact(LocalDate confirmedDate, BigDecimal companyAmount) {}
}
