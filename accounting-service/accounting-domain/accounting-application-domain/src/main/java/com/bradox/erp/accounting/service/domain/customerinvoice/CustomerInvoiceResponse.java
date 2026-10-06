package com.bradox.erp.accounting.service.domain.customerinvoice;

import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomerInvoiceResponse {
    private UUID id;
    private UUID companyId;
    private UUID customerPartnerId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String reference;
    private String currencyCode;
    private CustomerInvoiceState state;
    private CustomerInvoiceMoveType moveType;
    private UUID reversedInvoiceId;
    private UUID journalEntryId;
    private UUID salesOrderId;
    private BigDecimal exchangeRateToCompany;
    private BigDecimal orderDiscountAmount;
    private List<CustomerInvoiceLineResponse> lines = new ArrayList<>();
    private boolean openingBalance;
    /** Document total (lines + taxes − order discount), document currency. */
    private BigDecimal amountTotal;
    /** Active payment allocations (receipts for invoices, refunds for credit notes). */
    private BigDecimal amountPaid;
    /** Posted credit notes against this invoice (always zero for credit notes). */
    private BigDecimal amountCredited;
    /** amountTotal − amountPaid − amountCredited; only meaningful when POSTED. */
    private BigDecimal amountResidual;
    /** Invoices: money the customer paid beyond what is owed after credit notes, not yet refunded. */
    private BigDecimal amountOverpaid;
    /** Credit notes: how much can be paid back to the customer right now. */
    private BigDecimal amountRefundable;
    private List<CustomerPaymentAllocationResponse> paymentAllocations = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public CustomerInvoiceState getState() { return state; }
    public void setState(CustomerInvoiceState state) { this.state = state; }
    public CustomerInvoiceMoveType getMoveType() { return moveType; }
    public void setMoveType(CustomerInvoiceMoveType moveType) { this.moveType = moveType; }
    public UUID getReversedInvoiceId() { return reversedInvoiceId; }
    public void setReversedInvoiceId(UUID reversedInvoiceId) { this.reversedInvoiceId = reversedInvoiceId; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getSalesOrderId() { return salesOrderId; }
    public void setSalesOrderId(UUID salesOrderId) { this.salesOrderId = salesOrderId; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public BigDecimal getOrderDiscountAmount() { return orderDiscountAmount; }
    public void setOrderDiscountAmount(BigDecimal orderDiscountAmount) { this.orderDiscountAmount = orderDiscountAmount; }
    public List<CustomerInvoiceLineResponse> getLines() { return lines; }
    public void setLines(List<CustomerInvoiceLineResponse> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
    public boolean isOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(boolean openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getAmountTotal() { return amountTotal; }
    public void setAmountTotal(BigDecimal amountTotal) { this.amountTotal = amountTotal; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public BigDecimal getAmountCredited() { return amountCredited; }
    public void setAmountCredited(BigDecimal amountCredited) { this.amountCredited = amountCredited; }
    public BigDecimal getAmountResidual() { return amountResidual; }
    public void setAmountResidual(BigDecimal amountResidual) { this.amountResidual = amountResidual; }
    public BigDecimal getAmountOverpaid() { return amountOverpaid; }
    public void setAmountOverpaid(BigDecimal amountOverpaid) { this.amountOverpaid = amountOverpaid; }
    public BigDecimal getAmountRefundable() { return amountRefundable; }
    public void setAmountRefundable(BigDecimal amountRefundable) { this.amountRefundable = amountRefundable; }
    public List<CustomerPaymentAllocationResponse> getPaymentAllocations() { return paymentAllocations; }
    public void setPaymentAllocations(List<CustomerPaymentAllocationResponse> paymentAllocations) {
        this.paymentAllocations = paymentAllocations != null ? paymentAllocations : new ArrayList<>();
    }
}
