package com.bradox.erp.accounting.service.domain.ports.output;

import com.bradox.erp.accounting.service.domain.dashboard.AccountingDashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AccountingDashboardQueryPort {

    long countPostedInvoices(UUID companyId, LocalDate from, LocalDate to);

    long countPostedBills(UUID companyId, LocalDate from, LocalDate to);

    /** Net sales as on the Profit &amp; Loss: posted INCOME journal lines in the period. */
    BigDecimal sumPostedIncome(UUID companyId, LocalDate from, LocalDate to);

    /** {@link #sumPostedIncome} per entry date. */
    List<DocumentFact> listNetSalesFacts(UUID companyId, LocalDate from, LocalDate to);

    /** Customers by invoiced net sales (credit notes deducted) in the period. */
    List<AccountingDashboardResponse.RankedPartnerRow> topCustomers(UUID companyId, LocalDate from, LocalDate to, int limit);

    BigDecimal sumPostedSpend(UUID companyId, LocalDate from, LocalDate to);

    List<DocumentFact> listInvoiceFacts(UUID companyId, LocalDate from, LocalDate to);

    List<DocumentFact> listBillFacts(UUID companyId, LocalDate from, LocalDate to);

    List<AccountingDashboardResponse.RankedDocumentRow> topInvoices(UUID companyId, LocalDate from, LocalDate to, int limit);

    List<AccountingDashboardResponse.RankedDocumentRow> topBills(UUID companyId, LocalDate from, LocalDate to, int limit);

    record DocumentFact(LocalDate documentDate, BigDecimal companyAmount) {}
}
