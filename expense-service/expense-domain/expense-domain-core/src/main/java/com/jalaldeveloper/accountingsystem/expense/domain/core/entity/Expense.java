package com.jalaldeveloper.accountingsystem.expense.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.expense.domain.core.exception.ExpenseDomainException;
import com.jalaldeveloper.accountingsystem.expense.domain.core.valueobject.ExpenseId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Expense {

    private final ExpenseId id;
    private final CompanyId companyId;
    private String description;
    private UUID productId;
    private UUID accountId;
    private UUID employeeId;
    private UUID managerEmployeeId;
    private LocalDate expenseDate;
    private BigDecimal total;
    private BigDecimal taxAmount;
    private String currencyCode;
    private ReimbursementType reimbursement;
    private String notes;
    private ExpenseState state;
    private UUID journalEntryId;
    private UUID paymentJournalEntryId;
    private UUID paymentJournalId;
    private BigDecimal amountPaid;
    private LocalDate paymentDate;
    private String paymentReference;
    private long rowVersion;
    private Instant createdAt;
    private Instant updatedAt;

    private Expense(Builder b) {
        this.id = b.id;
        this.companyId = b.companyId;
        this.description = b.description;
        this.productId = b.productId;
        this.accountId = b.accountId;
        this.employeeId = b.employeeId;
        this.managerEmployeeId = b.managerEmployeeId;
        this.expenseDate = b.expenseDate;
        this.total = b.total != null ? b.total : BigDecimal.ZERO;
        this.taxAmount = b.taxAmount != null ? b.taxAmount : BigDecimal.ZERO;
        this.currencyCode = b.currencyCode;
        this.reimbursement = b.reimbursement != null ? b.reimbursement : ReimbursementType.EMPLOYEE;
        this.notes = b.notes;
        this.state = b.state != null ? b.state : ExpenseState.DRAFT;
        this.journalEntryId = b.journalEntryId;
        this.paymentJournalEntryId = b.paymentJournalEntryId;
        this.paymentJournalId = b.paymentJournalId;
        this.amountPaid = b.amountPaid != null ? b.amountPaid : BigDecimal.ZERO;
        this.paymentDate = b.paymentDate;
        this.paymentReference = b.paymentReference;
        this.rowVersion = b.rowVersion;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    public void validate() {
        if (companyId == null) throw new ExpenseDomainException("companyId required");
        if (description == null || description.isBlank()) throw new ExpenseDomainException("description required");
        if (employeeId == null) throw new ExpenseDomainException("employeeId required");
        if (expenseDate == null) throw new ExpenseDomainException("expenseDate required");
        if (currencyCode == null || currencyCode.isBlank()) throw new ExpenseDomainException("currencyCode required");
        if (total == null || total.signum() < 0) throw new ExpenseDomainException("total must be >= 0");
        if (taxAmount == null || taxAmount.signum() < 0) throw new ExpenseDomainException("taxAmount must be >= 0");
        if (taxAmount.compareTo(total) > 0) throw new ExpenseDomainException("taxAmount cannot exceed total");
    }

    public boolean isEditable() {
        return state == ExpenseState.DRAFT || state == ExpenseState.SUBMITTED;
    }

    public BigDecimal getAmountDue() {
        if (state == ExpenseState.PAID) {
            return BigDecimal.ZERO;
        }
        if (state != ExpenseState.POSTED && state != ExpenseState.APPROVED) {
            return BigDecimal.ZERO;
        }
        BigDecimal paid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        BigDecimal due = total.subtract(paid);
        return due.signum() > 0 ? due : BigDecimal.ZERO;
    }

    public void submit() {
        if (state != ExpenseState.DRAFT) {
            throw new ExpenseDomainException("Only draft expenses can be submitted");
        }
        if (total == null || total.signum() <= 0) {
            throw new ExpenseDomainException("total must be positive to submit");
        }
        this.state = ExpenseState.SUBMITTED;
        touch();
    }

    public void approve() {
        if (state != ExpenseState.SUBMITTED) {
            throw new ExpenseDomainException("Only submitted expenses can be approved");
        }
        this.state = ExpenseState.APPROVED;
        touch();
    }

    public void refuse() {
        if (state != ExpenseState.SUBMITTED) {
            throw new ExpenseDomainException("Only submitted expenses can be refused");
        }
        this.state = ExpenseState.DRAFT;
        touch();
    }

    public void markPosted(UUID journalEntryId) {
        if (state != ExpenseState.APPROVED) {
            throw new ExpenseDomainException("Only approved expenses can be posted");
        }
        if (accountId == null) {
            throw new ExpenseDomainException("accountId required to post");
        }
        if (total == null || total.signum() <= 0) {
            throw new ExpenseDomainException("total must be positive to post");
        }
        this.journalEntryId = journalEntryId;
        this.state = ExpenseState.POSTED;
        touch();
    }

    public void registerPayment(UUID paymentJournalEntryId, UUID paymentJournalId,
                                BigDecimal amount, LocalDate paymentDate, String paymentReference) {
        if (state != ExpenseState.POSTED) {
            throw new ExpenseDomainException("Only posted expenses can be paid");
        }
        if (journalEntryId == null) {
            throw new ExpenseDomainException("Expense must have a posted journal entry before payment");
        }
        if (paymentJournalEntryId == null || paymentJournalId == null) {
            throw new ExpenseDomainException("payment journal required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new ExpenseDomainException("payment amount must be positive");
        }
        BigDecimal due = getAmountDue();
        if (amount.compareTo(due) > 0) {
            throw new ExpenseDomainException("payment amount exceeds amount due");
        }
        this.paymentJournalEntryId = paymentJournalEntryId;
        this.paymentJournalId = paymentJournalId;
        this.amountPaid = (amountPaid != null ? amountPaid : BigDecimal.ZERO).add(amount);
        this.paymentDate = paymentDate;
        this.paymentReference = paymentReference;
        if (this.amountPaid.compareTo(total) >= 0) {
            this.state = ExpenseState.PAID;
        }
        touch();
    }

    public void cancel() {
        if (state != ExpenseState.DRAFT && state != ExpenseState.SUBMITTED) {
            throw new ExpenseDomainException("Only draft or submitted expenses can be cancelled");
        }
        this.state = ExpenseState.CANCELLED;
        touch();
    }

    public void applyUpdate(String description, UUID productId, UUID accountId, UUID employeeId,
                            UUID managerEmployeeId, LocalDate expenseDate, BigDecimal total,
                            BigDecimal taxAmount, String currencyCode, ReimbursementType reimbursement,
                            String notes) {
        if (!isEditable()) {
            throw new ExpenseDomainException("Expense is not editable in state " + state);
        }
        this.description = description;
        this.productId = productId;
        this.accountId = accountId;
        this.employeeId = employeeId;
        this.managerEmployeeId = managerEmployeeId;
        this.expenseDate = expenseDate;
        this.total = total != null ? total : BigDecimal.ZERO;
        this.taxAmount = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        this.currencyCode = currencyCode;
        this.reimbursement = reimbursement != null ? reimbursement : ReimbursementType.EMPLOYEE;
        this.notes = notes;
        touch();
        validate();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public ExpenseId getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public String getDescription() { return description; }
    public UUID getProductId() { return productId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getManagerEmployeeId() { return managerEmployeeId; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public ReimbursementType getReimbursement() { return reimbursement; }
    public String getNotes() { return notes; }
    public ExpenseState getState() { return state; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public UUID getPaymentJournalEntryId() { return paymentJournalEntryId; }
    public UUID getPaymentJournalId() { return paymentJournalId; }
    public BigDecimal getAmountPaid() { return amountPaid != null ? amountPaid : BigDecimal.ZERO; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getPaymentReference() { return paymentReference; }
    public long getRowVersion() { return rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public BigDecimal getNetAmount() {
        return total.subtract(taxAmount != null ? taxAmount : BigDecimal.ZERO);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ExpenseId id;
        private CompanyId companyId;
        private String description;
        private UUID productId;
        private UUID accountId;
        private UUID employeeId;
        private UUID managerEmployeeId;
        private LocalDate expenseDate;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal taxAmount = BigDecimal.ZERO;
        private String currencyCode;
        private ReimbursementType reimbursement = ReimbursementType.EMPLOYEE;
        private String notes;
        private ExpenseState state = ExpenseState.DRAFT;
        private UUID journalEntryId;
        private UUID paymentJournalEntryId;
        private UUID paymentJournalId;
        private BigDecimal amountPaid = BigDecimal.ZERO;
        private LocalDate paymentDate;
        private String paymentReference;
        private long rowVersion;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(ExpenseId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder productId(UUID v) { this.productId = v; return this; }
        public Builder accountId(UUID v) { this.accountId = v; return this; }
        public Builder employeeId(UUID v) { this.employeeId = v; return this; }
        public Builder managerEmployeeId(UUID v) { this.managerEmployeeId = v; return this; }
        public Builder expenseDate(LocalDate v) { this.expenseDate = v; return this; }
        public Builder total(BigDecimal v) { this.total = v; return this; }
        public Builder taxAmount(BigDecimal v) { this.taxAmount = v; return this; }
        public Builder currencyCode(String v) { this.currencyCode = v; return this; }
        public Builder reimbursement(ReimbursementType v) { this.reimbursement = v; return this; }
        public Builder notes(String v) { this.notes = v; return this; }
        public Builder state(ExpenseState v) { this.state = v; return this; }
        public Builder journalEntryId(UUID v) { this.journalEntryId = v; return this; }
        public Builder paymentJournalEntryId(UUID v) { this.paymentJournalEntryId = v; return this; }
        public Builder paymentJournalId(UUID v) { this.paymentJournalId = v; return this; }
        public Builder amountPaid(BigDecimal v) { this.amountPaid = v; return this; }
        public Builder paymentDate(LocalDate v) { this.paymentDate = v; return this; }
        public Builder paymentReference(String v) { this.paymentReference = v; return this; }
        public Builder rowVersion(long v) { this.rowVersion = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder updatedAt(Instant v) { this.updatedAt = v; return this; }

        public Expense build() { return new Expense(this); }
    }
}
