package com.jalaldeveloper.accountingsystem.platform.settings;

import java.util.UUID;

/**
 * Hook implemented by the accounting module so the platform can keep a company's
 * {@code default_currency} and the accounting base currency in sync. When the
 * company's default currency changes, the accounting side re-points its base
 * currency to match — or rejects the change when transactions already exist.
 */
public interface BaseCurrencyChangeHandler {

    /**
     * Aligns the accounting base currency with {@code currencyCode}. No-op when already
     * aligned. Throws (409) when the company already has ledger activity.
     */
    void changeBaseCurrency(UUID companyId, String currencyCode);
}
