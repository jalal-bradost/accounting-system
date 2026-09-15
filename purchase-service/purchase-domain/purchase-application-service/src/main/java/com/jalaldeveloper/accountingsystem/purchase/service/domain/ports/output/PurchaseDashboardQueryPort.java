package com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output;

import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.PurchaseDashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PurchaseDashboardQueryPort {

    long countRfqs(UUID companyId, LocalDate from, LocalDate to);

    long countConfirmedOrders(UUID companyId, LocalDate from, LocalDate to);

    BigDecimal sumConfirmedSpend(UUID companyId, LocalDate from, LocalDate to);

    List<ConfirmedOrderFact> listConfirmedOrderFacts(UUID companyId, LocalDate from, LocalDate to);

    List<PurchaseDashboardResponse.RankedOrderRow> topRfqs(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<PurchaseDashboardResponse.RankedOrderRow> topConfirmedOrders(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<PurchaseDashboardResponse.RankedProductRow> topProducts(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<PurchaseDashboardResponse.CategoryNode> topCategories(UUID companyId, LocalDate from, LocalDate to, int limit);

    record ConfirmedOrderFact(LocalDate confirmedDate, BigDecimal companyAmount) {}
}
