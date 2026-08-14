package com.jalaldeveloper.accountingsystem.expense.service.domain.dto;

import java.math.BigDecimal;

public class ExpenseSummaryResponse {

    private BigDecimal toSubmitTotal = BigDecimal.ZERO;
    private BigDecimal waitingApprovalTotal = BigDecimal.ZERO;
    private BigDecimal waitingReimbursementTotal = BigDecimal.ZERO;

    public ExpenseSummaryResponse() {}

    public ExpenseSummaryResponse(BigDecimal toSubmitTotal,
                                  BigDecimal waitingApprovalTotal,
                                  BigDecimal waitingReimbursementTotal) {
        this.toSubmitTotal = toSubmitTotal != null ? toSubmitTotal : BigDecimal.ZERO;
        this.waitingApprovalTotal = waitingApprovalTotal != null ? waitingApprovalTotal : BigDecimal.ZERO;
        this.waitingReimbursementTotal = waitingReimbursementTotal != null ? waitingReimbursementTotal : BigDecimal.ZERO;
    }

    public BigDecimal getToSubmitTotal() { return toSubmitTotal; }
    public void setToSubmitTotal(BigDecimal toSubmitTotal) { this.toSubmitTotal = toSubmitTotal; }
    public BigDecimal getWaitingApprovalTotal() { return waitingApprovalTotal; }
    public void setWaitingApprovalTotal(BigDecimal waitingApprovalTotal) {
        this.waitingApprovalTotal = waitingApprovalTotal;
    }
    public BigDecimal getWaitingReimbursementTotal() { return waitingReimbursementTotal; }
    public void setWaitingReimbursementTotal(BigDecimal waitingReimbursementTotal) {
        this.waitingReimbursementTotal = waitingReimbursementTotal;
    }
}
