package com.jalaldeveloper.accountingsystem.expense.service.domain.dto;

import com.jalaldeveloper.accountingsystem.expense.domain.core.entity.ExpenseState;
import com.jalaldeveloper.accountingsystem.expense.domain.core.entity.ReimbursementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ExpenseResponse {

    private UUID id;
    private UUID companyId;
    private String description;
    private UUID productId;
    private UUID accountId;
    private UUID employeeId;
    private String employeeName;
    private String employeeImageUrl;
    private UUID managerEmployeeId;
    private String managerName;
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
    private BigDecimal amountDue;
    private LocalDate paymentDate;
    private String paymentReference;
    private long rowVersion;
    private Instant createdAt;
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
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmployeeImageUrl() { return employeeImageUrl; }
    public void setEmployeeImageUrl(String employeeImageUrl) { this.employeeImageUrl = employeeImageUrl; }
    public UUID getManagerEmployeeId() { return managerEmployeeId; }
    public void setManagerEmployeeId(UUID managerEmployeeId) { this.managerEmployeeId = managerEmployeeId; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
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
    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
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
