package com.jalaldeveloper.accountingsystem.contacts.domain.core.entity;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.exception.ContactsDomainException;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PaymentTermsId;
import com.jalaldeveloper.accountingsystem.domain.entity.ArchivableAggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Simple payment terms aggregate (e.g. "Net 30", "2/10 Net 30"). Odoo's account.payment.term
 * supports multi-line terms; we model the common single-line case and leave room to extend.
 */
public class PaymentTerms extends ArchivableAggregateRoot<PaymentTermsId> {

    private final CompanyId companyId;
    private final String name;
    private final int daysNet;
    private final int discountDays;
    private final BigDecimal discountPercent;

    private PaymentTerms(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.name = b.name;
        this.daysNet = b.daysNet;
        this.discountDays = b.discountDays;
        this.discountPercent = b.discountPercent != null ? b.discountPercent : BigDecimal.ZERO;
        if (b.archived) {
            super.restoreArchiveState(false, b.archivedAt, b.archivedBy);
        }
    }

    public void validate() {
        if (companyId == null) throw new ContactsDomainException("companyId required");
        if (name == null || name.isBlank()) throw new ContactsDomainException("name required");
        if (daysNet < 0) throw new ContactsDomainException("daysNet must be >= 0");
        if (discountDays < 0 || discountDays > daysNet) {
            throw new ContactsDomainException("discountDays must be between 0 and daysNet");
        }
        if (discountPercent.signum() < 0 || discountPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ContactsDomainException("discountPercent must be in [0,100]");
        }
    }

    public CompanyId getCompanyId() { return companyId; }
    public String getName() { return name; }
    public int getDaysNet() { return daysNet; }
    public int getDiscountDays() { return discountDays; }
    public BigDecimal getDiscountPercent() { return discountPercent; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private PaymentTermsId id;
        private CompanyId companyId;
        private String name;
        private int daysNet;
        private int discountDays;
        private BigDecimal discountPercent;
        private boolean archived;
        private Instant archivedAt;
        private String archivedBy;

        public Builder id(PaymentTermsId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder daysNet(int v) { this.daysNet = v; return this; }
        public Builder discountDays(int v) { this.discountDays = v; return this; }
        public Builder discountPercent(BigDecimal v) { this.discountPercent = v; return this; }
        public Builder archived(boolean v) { this.archived = v; return this; }
        public Builder archivedAt(Instant v) { this.archivedAt = v; return this; }
        public Builder archivedBy(String v) { this.archivedBy = v; return this; }
        public PaymentTerms build() { return new PaymentTerms(this); }
    }
}
