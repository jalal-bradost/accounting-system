package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.dashboard.AccountingDashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AccountingDashboardQueryPort {

    long countPostedInvoices(UUID companyId, LocalDate from, LocalDate to);

    long countPostedBills(UUID companyId, LocalDate from, LocalDate to);

    BigDecimal sumPostedIncome(UUID companyId, LocalDate from, LocalDate to);

    BigDecimal sumPostedSpend(UUID companyId, LocalDate from, LocalDate to);

    List<DocumentFact> listInvoiceFacts(UUID companyId, LocalDate from, LocalDate to);

    List<DocumentFact> listBillFacts(UUID companyId, LocalDate from, LocalDate to);

    List<AccountingDashboardResponse.RankedDocumentRow> topInvoices(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<AccountingDashboardResponse.RankedDocumentRow> topBills(UUID companyId, LocalDate from, LocalDate to, int limit);

    record DocumentFact(LocalDate documentDate, BigDecimal companyAmount) {}
}
