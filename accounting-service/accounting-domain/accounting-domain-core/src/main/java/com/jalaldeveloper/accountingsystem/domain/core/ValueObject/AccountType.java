package com.jalaldeveloper.accountingsystem.domain.core.ValueObject;

import java.util.Arrays;
import java.util.List;

/**
 * Granular account types modelled after Odoo's chart of accounts. Each value belongs to a
 * single {@link Category} that drives report grouping (Balance Sheet vs P&L).
 */
public enum AccountType {
    // Balance Sheet — Assets
    RECEIVABLE(Category.ASSET),
    BANK_AND_CASH(Category.ASSET),
    CURRENT_ASSETS(Category.ASSET),
    NON_CURRENT_ASSETS(Category.ASSET),
    PREPAYMENTS(Category.ASSET),
    FIXED_ASSETS(Category.ASSET),

    // Balance Sheet — Liabilities
    PAYABLE(Category.LIABILITY),
    CREDIT_CARD(Category.LIABILITY),
    CURRENT_LIABILITIES(Category.LIABILITY),
    NON_CURRENT_LIABILITIES(Category.LIABILITY),

    // Balance Sheet — Equity
    EQUITY(Category.EQUITY),
    CURRENT_YEAR_EARNINGS(Category.EQUITY),

    // Profit & Loss — Income
    INCOME(Category.INCOME),
    OTHER_INCOME(Category.INCOME),

    // Profit & Loss — Expense
    EXPENSES(Category.EXPENSE),
    OTHER_EXPENSES(Category.EXPENSE),
    DEPRECIATION(Category.EXPENSE),
    COST_OF_REVENUE(Category.EXPENSE),

    // Other
    OFF_BALANCE_SHEET(Category.OFF_BALANCE);

    public enum Category {
        ASSET,
        LIABILITY,
        EQUITY,
        INCOME,
        EXPENSE,
        OFF_BALANCE
    }

    private final Category category;

    AccountType(Category category) {
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    /** Returns every {@link AccountType} that belongs to one of the given categories. */
    public static List<AccountType> typesIn(Category... categories) {
        List<Category> requested = List.of(categories);
        return Arrays.stream(values())
                .filter(t -> requested.contains(t.category))
                .toList();
    }
}
