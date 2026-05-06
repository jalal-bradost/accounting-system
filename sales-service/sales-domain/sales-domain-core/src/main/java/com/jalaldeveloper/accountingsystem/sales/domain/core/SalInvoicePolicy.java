package com.jalaldeveloper.accountingsystem.sales.domain.core;

/** When invoice lines can be created for a stock/non-stock line. */
public enum SalInvoicePolicy {
    /** Invoice after delivered quantity (default for storable products). */
    DELIVERED,
    /** Invoice on ordered quantity (services, or explicit override). */
    ORDERED
}
