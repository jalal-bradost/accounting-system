package com.jalaldeveloper.accountingsystem.expense.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.expense.domain.core.entity.ExpenseState;
import com.jalaldeveloper.accountingsystem.expense.domain.core.entity.ReimbursementType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "exp_expense")
public class ExpExpenseEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "manager_employee_id")
    private UUID managerEmployeeId;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 8)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReimbursementType reimbursement = ReimbursementType.EMPLOYEE;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExpenseState state = ExpenseState.DRAFT;

    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

    @Column(name = "payment_journal_entry_id")
    private UUID paymentJournalEntryId;

    @Column(name = "payment_journal_id")
    private UUID paymentJournalId;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getManagerEmployeeId() { return managerEmployeeId; }
    public void setManagerEmployeeId(UUID managerEmployeeId) { this.managerEmployeeId = managerEmployeeId; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public ReimbursementType getReimbursement() { return reimbursement; }
    public void setReimbursement(ReimbursementType reimbursement) { this.reimbursement = reimbursement; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public ExpenseState getState() { return state; }
    public void setState(ExpenseState state) { this.state = state; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getPaymentJournalEntryId() { return paymentJournalEntryId; }
    public void setPaymentJournalEntryId(UUID paymentJournalEntryId) { this.paymentJournalEntryId = paymentJournalEntryId; }
    public UUID getPaymentJournalId() { return paymentJournalId; }
    public void setPaymentJournalId(UUID paymentJournalId) { this.paymentJournalId = paymentJournalId; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public long getRowVersion() { return rowVersion; }
    public void setRowVersion(long rowVersion) { this.rowVersion = rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
