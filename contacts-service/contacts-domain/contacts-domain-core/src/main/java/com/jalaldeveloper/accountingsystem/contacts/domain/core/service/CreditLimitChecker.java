package com.jalaldeveloper.accountingsystem.contacts.domain.core.service;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.Partner;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.exception.ContactsDomainException;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;

/**
 * Pure domain rule for credit-limit checks. Stateless; takes the partner aggregate
 * plus the currently outstanding receivable balance (sourced from accounting via
 * an output port at the application layer).
 *
 * <p>A credit limit of zero is treated as <b>unlimited</b>, matching Odoo's
 * convention.
 */
public final class CreditLimitChecker {

    private CreditLimitChecker() {}

    public record CreditStatus(Money creditLimit, Money outstandingReceivable, Money available, boolean unlimited) {}

    public static CreditStatus check(Partner partner, Money outstandingReceivable) {
        if (partner == null) throw new ContactsDomainException("partner required");
        if (!partner.isCustomer()) {
            throw new ContactsDomainException("partner is not a customer: " + partner.getId().getId());
        }
        Money limit = partner.getCreditLimit() != null ? partner.getCreditLimit() : Money.ZERO;
        Money outstanding = outstandingReceivable != null ? outstandingReceivable : Money.ZERO;

        if (limit.getAmount().signum() == 0) {
            return new CreditStatus(limit, outstanding, Money.ZERO, true);
        }
        Money available = limit.subtract(outstanding);
        return new CreditStatus(limit, outstanding, available, false);
    }

    /** Throws when the requested amount would exceed the available credit. No-op when unlimited. */
    public static void requireWithinLimit(Partner partner, Money outstandingReceivable, Money requested) {
        if (requested == null || requested.getAmount().signum() <= 0) return;
        CreditStatus status = check(partner, outstandingReceivable);
        if (status.unlimited()) return;
        Money projected = status.outstandingReceivable().add(requested);
        if (projected.isGreaterThan(status.creditLimit())) {
            throw new ContactsDomainException(
                    "Credit limit exceeded: outstanding " + status.outstandingReceivable().getAmount()
                            + " + requested " + requested.getAmount()
                            + " exceeds limit " + status.creditLimit().getAmount());
        }
    }
}
