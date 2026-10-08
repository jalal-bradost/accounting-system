package com.bradox.erp.sales.domain.core;

/** When invoice lines can be created for a stock/non-stock line. */
public enum SalInvoicePolicy {
    /** Invoice after delivered quantity (default for storable products). */
    DELIVERED,
    /** Invoice on ordered quantity (services, or explicit override). */
    ORDERED,
    /**
     * Service lines billed from approved timesheet hours (TSH-06). The delivered quantity is set by the
     * Timesheet module, never by stock moves, and invoicing follows it like a delivered line.
     */
    TIMESHEET
}
