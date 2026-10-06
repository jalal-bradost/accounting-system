package com.bradox.erp.domain.core.ValueObject;

/** Distinguishes normal customer receipts from refunds against credit notes. */
public enum CustomerPaymentKind {
    PAYMENT,
    REFUND
}
