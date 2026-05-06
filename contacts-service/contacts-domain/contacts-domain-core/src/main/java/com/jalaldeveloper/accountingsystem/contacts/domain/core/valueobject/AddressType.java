package com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject;

/** Postal-address role on a partner, mirroring Odoo res.partner.type. */
public enum AddressType {
    /** Generic / contact address. */
    CONTACT,
    /** Billing / invoice address. */
    INVOICE,
    /** Shipping / delivery address. */
    DELIVERY,
    /** Other (e.g. private). */
    OTHER
}
