package com.bradox.erp.domain.core.ValueObject;

public enum CustomerInvoiceState {
    DRAFT,
    POSTED,
    /** Posted then voided by a reversing journal entry; excluded from every financial sum. */
    CANCELLED
}
