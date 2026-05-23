package com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One row on a partner statement. Exactly one of the document id pairs is non-null,
 * depending on {@link #lineType}.
 */
public class PartnerStatementLineResponse {

    /** CUSTOMER_INVOICE, CUSTOMER_PAYMENT, VENDOR_BILL, VENDOR_PAYMENT */
    private String lineType;
    private LocalDate entryDate;
    private String reference;
    private String currencyCode;
    private UUID customerInvoiceId;
    private UUID customerPaymentId;
    private UUID vendorBillId;
    private UUID vendorPaymentId;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public UUID getCustomerInvoiceId() {
        return customerInvoiceId;
    }

    public void setCustomerInvoiceId(UUID customerInvoiceId) {
        this.customerInvoiceId = customerInvoiceId;
    }

    public UUID getCustomerPaymentId() {
        return customerPaymentId;
    }

    public void setCustomerPaymentId(UUID customerPaymentId) {
        this.customerPaymentId = customerPaymentId;
    }

    public UUID getVendorBillId() {
        return vendorBillId;
    }

    public void setVendorBillId(UUID vendorBillId) {
        this.vendorBillId = vendorBillId;
    }

    public UUID getVendorPaymentId() {
        return vendorPaymentId;
    }

    public void setVendorPaymentId(UUID vendorPaymentId) {
        this.vendorPaymentId = vendorPaymentId;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
