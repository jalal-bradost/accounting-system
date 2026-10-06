package com.bradox.erp.accounting.service.domain.ports.output;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Open balances in foreign currencies on receivable and payable accounts, read from the ledger. */
public interface CurrencyRevaluationQueryPort {

    /**
     * Per account, partner and currency: the balance in the foreign currency and what it is carried at
     * in the company currency, over posted entries dated up to the end of {@code asOf}.
     */
    List<OpenForeignBalance> findOpenForeignBalances(UUID companyId, LocalDate asOf, String baseCurrency);

    record OpenForeignBalance(
            UUID accountId,
            String accountCode,
            String accountName,
            UUID partnerId,
            String partnerName,
            String currencyCode,
            /** Debit-positive balance in the foreign currency. */
            BigDecimal foreignBalance,
            /** Debit-positive balance carried in the company currency. */
            BigDecimal companyBalance) {}
}
