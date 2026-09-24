package com.bradox.delin.contacts.domain.core.valueobject;

/** Whether a partner represents a legal entity or a natural person, mirroring Odoo's res.partner.is_company. */
public enum PartnerKind {
    COMPANY,
    INDIVIDUAL
}
