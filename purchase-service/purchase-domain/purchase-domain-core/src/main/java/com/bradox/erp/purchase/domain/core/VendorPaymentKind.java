package com.bradox.erp.purchase.domain.core;

/** Distinguishes normal vendor payouts from refunds against credit notes. */
public enum VendorPaymentKind {
    PAYOUT,
    REFUND
}
