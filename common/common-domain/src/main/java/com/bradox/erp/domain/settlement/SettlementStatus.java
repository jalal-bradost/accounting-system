package com.bradox.erp.domain.settlement;

/**
 * Payment settlement status for a sales or purchase order.
 */
public enum SettlementStatus {
    CANCELLED,
    NEW,
    UNPAID,
    PARTIAL_PAID,
    PAID,
    TO_REFUND
}
