package com.bradox.erp.accounting.service.domain.ports.output;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** What customers owe, read from the receivable accounts of the ledger. */
public interface CustomerBalanceQueryPort {

    /**
     * Balance per customer in the company currency (debit minus credit on receivable accounts over
     * posted entries). Positive means the customer owes us. {@code partnerIds} null or empty means
     * every customer with a non-zero balance.
     */
    List<CustomerBalance> findBalances(UUID companyId, Collection<UUID> partnerIds);

    record CustomerBalance(UUID partnerId, BigDecimal balance) {}
}
