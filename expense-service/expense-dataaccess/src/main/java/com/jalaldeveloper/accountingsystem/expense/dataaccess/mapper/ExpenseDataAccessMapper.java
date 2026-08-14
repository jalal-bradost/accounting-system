package com.jalaldeveloper.accountingsystem.expense.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.expense.dataaccess.entity.ExpExpenseEntity;
import com.jalaldeveloper.accountingsystem.expense.domain.core.entity.Expense;
import com.jalaldeveloper.accountingsystem.expense.domain.core.valueobject.ExpenseId;

import java.math.BigDecimal;

public final class ExpenseDataAccessMapper {

    private ExpenseDataAccessMapper() {}

    public static Expense entityToDomain(ExpExpenseEntity e) {
        if (e == null) return null;
        return Expense.builder()
                .id(new ExpenseId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .description(e.getDescription())
                .productId(e.getProductId())
                .accountId(e.getAccountId())
                .employeeId(e.getEmployeeId())
                .managerEmployeeId(e.getManagerEmployeeId())
                .expenseDate(e.getExpenseDate())
                .total(e.getTotal())
                .taxAmount(e.getTaxAmount())
                .currencyCode(e.getCurrencyCode())
                .reimbursement(e.getReimbursement())
                .notes(e.getNotes())
                .state(e.getState())
                .journalEntryId(e.getJournalEntryId())
                .paymentJournalEntryId(e.getPaymentJournalEntryId())
                .paymentJournalId(e.getPaymentJournalId())
                .amountPaid(e.getAmountPaid() != null ? e.getAmountPaid() : BigDecimal.ZERO)
                .paymentDate(e.getPaymentDate())
                .paymentReference(e.getPaymentReference())
                .rowVersion(e.getRowVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public static ExpExpenseEntity domainToEntity(Expense d, ExpExpenseEntity existing) {
        ExpExpenseEntity e = existing != null ? existing : new ExpExpenseEntity();
        e.setId(d.getId().getId());
        e.setCompanyId(d.getCompanyId().getId());
        e.setDescription(d.getDescription());
        e.setProductId(d.getProductId());
        e.setAccountId(d.getAccountId());
        e.setEmployeeId(d.getEmployeeId());
        e.setManagerEmployeeId(d.getManagerEmployeeId());
        e.setExpenseDate(d.getExpenseDate());
        e.setTotal(d.getTotal());
        e.setTaxAmount(d.getTaxAmount());
        e.setCurrencyCode(d.getCurrencyCode());
        e.setReimbursement(d.getReimbursement());
        e.setNotes(d.getNotes());
        e.setState(d.getState());
        e.setJournalEntryId(d.getJournalEntryId());
        e.setPaymentJournalEntryId(d.getPaymentJournalEntryId());
        e.setPaymentJournalId(d.getPaymentJournalId());
        e.setAmountPaid(d.getAmountPaid());
        e.setPaymentDate(d.getPaymentDate());
        e.setPaymentReference(d.getPaymentReference());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        if (existing == null) {
            e.setRowVersion(d.getRowVersion());
        }
        return e;
    }
}
