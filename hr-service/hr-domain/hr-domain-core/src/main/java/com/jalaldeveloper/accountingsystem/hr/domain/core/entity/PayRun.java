package com.jalaldeveloper.accountingsystem.hr.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PayRun {

    public static final String STATE_DRAFT = "draft";
    public static final String STATE_COMPUTED = "computed";
    public static final String STATE_POSTED = "posted";
    public static final String STATE_PAID = "paid";

    private final UUID id;
    private final CompanyId companyId;
    private final String name;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final String state;
    private final UUID journalEntryId;
    private final UUID paymentJournalEntryId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<Payslip> payslips;

    private PayRun(Builder b) {
        this.id = b.id != null ? b.id : UUID.randomUUID();
        this.companyId = b.companyId;
        this.name = b.name;
        this.periodStart = b.periodStart;
        this.periodEnd = b.periodEnd;
        this.state = b.state != null ? b.state : STATE_DRAFT;
        this.journalEntryId = b.journalEntryId;
        this.paymentJournalEntryId = b.paymentJournalEntryId;
        this.createdAt = b.createdAt != null ? b.createdAt : Instant.now();
        this.updatedAt = b.updatedAt != null ? b.updatedAt : Instant.now();
        this.payslips = b.payslips != null ? List.copyOf(b.payslips) : List.of();
    }

    public void validate() {
        if (companyId == null) throw new HrDomainException("companyId required");
        if (name == null || name.isBlank()) throw new HrDomainException("name required");
        if (periodStart == null || periodEnd == null) throw new HrDomainException("period required");
        if (periodEnd.isBefore(periodStart)) throw new HrDomainException("periodEnd must be on or after periodStart");
    }

    public PayRun withPayslips(List<Payslip> newPayslips) {
        return toBuilder()
                .payslips(newPayslips)
                .state(STATE_COMPUTED)
                .updatedAt(Instant.now())
                .build();
    }

    public PayRun markPosted(UUID journalEntryId) {
        if (!STATE_COMPUTED.equals(state)) {
            throw new HrDomainException("Only computed pay runs can be posted");
        }
        return toBuilder()
                .state(STATE_POSTED)
                .journalEntryId(journalEntryId)
                .updatedAt(Instant.now())
                .build();
    }

    public PayRun markPaid(UUID paymentJournalEntryId) {
        if (!STATE_POSTED.equals(state)) {
            throw new HrDomainException("Only posted pay runs can be paid");
        }
        return toBuilder()
                .state(STATE_PAID)
                .paymentJournalEntryId(paymentJournalEntryId)
                .updatedAt(Instant.now())
                .build();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .companyId(companyId)
                .name(name)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .state(state)
                .journalEntryId(journalEntryId)
                .paymentJournalEntryId(paymentJournalEntryId)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .payslips(new ArrayList<>(payslips));
    }

    public UUID getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public String getName() { return name; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public String getState() { return state; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public UUID getPaymentJournalEntryId() { return paymentJournalEntryId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Payslip> getPayslips() { return Collections.unmodifiableList(payslips); }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private CompanyId companyId;
        private String name;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String state;
        private UUID journalEntryId;
        private UUID paymentJournalEntryId;
        private Instant createdAt;
        private Instant updatedAt;
        private List<Payslip> payslips = new ArrayList<>();

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder periodStart(LocalDate v) { this.periodStart = v; return this; }
        public Builder periodEnd(LocalDate v) { this.periodEnd = v; return this; }
        public Builder state(String v) { this.state = v; return this; }
        public Builder journalEntryId(UUID v) { this.journalEntryId = v; return this; }
        public Builder paymentJournalEntryId(UUID v) { this.paymentJournalEntryId = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder updatedAt(Instant v) { this.updatedAt = v; return this; }
        public Builder payslips(List<Payslip> v) { this.payslips = v; return this; }
        public PayRun build() { return new PayRun(this); }
    }
}
