package com.bradox.delin.purchase.domain.core;

public enum VendorBillMoveType {
    BILL,
    CREDIT_NOTE,
    /** Extra vendor charge after a posted bill (increases AP). */
    DEBIT_NOTE
}
