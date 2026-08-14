package com.jalaldeveloper.accountingsystem.expense.service.domain.dto;

import com.jalaldeveloper.accountingsystem.expense.domain.core.entity.ReimbursementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class SaveExpenseCommand {

    private UUID companyId;

    @NotBlank
    private String description;

    private UUID productId;
    private UUID accountId;

    @NotNull
    private UUID employeeId;

    private UUID managerEmployeeId;

    @NotNull
    private LocalDate expenseDate;

    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private String currencyCode;
    private ReimbursementType reimbursement = ReimbursementType.EMPLOYEE;
    private String notes;

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
}
