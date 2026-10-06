package com.bradox.erp.accounting.service.domain.ports.input.service;

import com.bradox.erp.accounting.service.domain.customerinvoice.AllocateCustomerPaymentCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CreateCreditNoteFromInvoiceCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CreateCustomerInvoiceCommand;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerPaymentResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerInvoiceApplicationService {

    boolean hasPostedInvoiceForSalesOrder(UUID salesOrderId);

    /**
     * Quantity still creditable per line of a posted invoice: line qty minus what credit notes
     * (draft or posted) against that invoice already cover. Keyed by invoice line id.
     */
    Map<UUID, BigDecimal> remainingCreditableQtyByInvoiceLine(UUID invoiceId);

    /** Deletes a draft invoice or credit note. Posted documents are never deleted. */
    void deleteDraftCustomerInvoice(UUID invoiceId);

    /** Deletes every draft invoice and credit note of a sales order; returns how many were removed. */
    int deleteDraftDocumentsForSalesOrder(UUID salesOrderId);

    /** Quantities reserved on draft customer invoices keyed by sales order line id. */
    Map<UUID, BigDecimal> draftAllocatedQtyBySalesOrderLine(UUID salesOrderId);

    /** Non-gift quantities reserved on draft customer invoices keyed by sales order line id. */
    Map<UUID, BigDecimal> draftAllocatedChargeQtyBySalesOrderLine(UUID salesOrderId);

    /** Quantities reserved on draft customer credit notes keyed by sales order line id. */
    Map<UUID, BigDecimal> draftCreditNoteAllocatedQtyBySalesOrderLine(UUID salesOrderId);

    /** Non-gift quantities reserved on draft customer credit notes keyed by sales order line id. */
    Map<UUID, BigDecimal> draftCreditNoteAllocatedChargeQtyBySalesOrderLine(UUID salesOrderId);

    /**
     * Posted invoice/CN qty nets by sales order line id.
     * Charge = non-gift invoice qty − non-gift credit-note qty.
     * Gift = gift invoice qty − gift credit-note qty.
     */
    PostedSoLineQtyNets postedQtyNetsBySalesOrderLine(UUID salesOrderId);

    /** Posted charge vs gift qty nets for sales-order lines. */
    record PostedSoLineQtyNets(Map<UUID, BigDecimal> chargeNet, Map<UUID, BigDecimal> giftNet) {
        public PostedSoLineQtyNets {
            chargeNet = chargeNet != null ? chargeNet : Map.of();
            giftNet = giftNet != null ? giftNet : Map.of();
        }

        public BigDecimal chargeNetFor(UUID salesOrderLineId) {
            return chargeNet.getOrDefault(salesOrderLineId, BigDecimal.ZERO);
        }

        public BigDecimal giftNetFor(UUID salesOrderLineId) {
            return giftNet.getOrDefault(salesOrderLineId, BigDecimal.ZERO);
        }
    }

    CustomerInvoiceResponse createCustomerInvoice(@Valid CreateCustomerInvoiceCommand command);

    CustomerInvoiceResponse createCreditNoteFromInvoice(UUID invoiceId, @Valid CreateCreditNoteFromInvoiceCommand command);

    List<CustomerInvoiceResponse> listCreditNotesForInvoice(UUID invoiceId);

    List<CustomerInvoiceResponse> listPostedInvoicesForSalesOrder(UUID salesOrderId);

    /**
     * Posted invoices and credit notes for a sales order (excludes drafts).
     * Used for order settlement calculation.
     */
    List<CustomerInvoiceResponse> listPostedDocumentsForSalesOrder(UUID salesOrderId);

    /** Posted invoices/CNs for many sales orders (batch settlement). */
    Map<UUID, List<CustomerInvoiceResponse>> listPostedDocumentsForSalesOrders(Collection<UUID> salesOrderIds);

    Page<CustomerInvoiceResponse> searchCustomerInvoices(UUID companyId, String state, UUID salesOrderId, String q, Pageable pageable);

    Page<CustomerPaymentResponse> searchCustomerPayments(UUID companyId, Pageable pageable);

    /**
     * Sum of active allocation amounts per invoice id.
     * Keys are invoice ids that have at least one active allocation; missing keys mean zero.
     */
    Map<UUID, BigDecimal> sumPostedPaymentsByInvoiceIds(java.util.Collection<UUID> invoiceIds);

    CustomerInvoiceResponse postCustomerInvoice(UUID invoiceId);

    /**
     * Creates and posts an opening-balance customer invoice on the OPEN journal
     * (Dr AR partner / Cr Opening Balance Equity). Sequence {@code OB-INV}.
     */
    CustomerInvoiceResponse createOpeningCustomerInvoice(UUID companyId, UUID partnerId, BigDecimal amount,
                                                         String currency, LocalDate date, LocalDate dueDate,
                                                         String reference, UUID openingJournalId,
                                                         UUID openingEquityAccountId);

    /** Reverse the JE and set CANCELLED on an opening customer invoice. */
    void cancelOpeningCustomerInvoice(UUID invoiceId);

    CustomerInvoiceResponse getCustomerInvoice(UUID invoiceId);

    List<CustomerInvoiceResponse> listCustomerInvoices(UUID companyId);

    List<CustomerPaymentResponse> listCustomerPayments(UUID companyId);

    CustomerPaymentResponse getCustomerPayment(UUID paymentId);

    CustomerPaymentResponse registerCustomerPayment(@Valid RegisterCustomerPaymentCommand command);

    CustomerPaymentResponse allocateCustomerPayment(UUID paymentId, @Valid AllocateCustomerPaymentCommand command);

    CustomerPaymentResponse deallocateCustomerPayment(UUID allocationId);

    CustomerPaymentResponse reverseCustomerPayment(UUID paymentId, String reason);

    /** Reverses a wrong payment and registers the corrected one in the same step. */
    CustomerPaymentResponse correctCustomerPayment(
            UUID paymentId, com.bradox.erp.accounting.service.domain.customerinvoice.CorrectCustomerPaymentCommand command);

    /** Pays back the credit the customer has on a paid invoice that was credited. */
    CustomerPaymentResponse refundCustomerCredit(
            UUID invoiceId, com.bradox.erp.accounting.service.domain.customerinvoice.RefundCustomerCreditCommand command);

    /** Moves that credit off the invoice into the customer's open receipts; returns the amount. */
    BigDecimal keepCustomerCredit(UUID invoiceId);

    /** Settles the invoice with the customer's open receipts; returns the amount applied. */
    BigDecimal applyCustomerCredit(UUID invoiceId);

    /** Like {@link #applyCustomerCredit} but at most {@code limit}, and applying nothing is not an error. */
    BigDecimal applyCustomerCreditUpTo(UUID invoiceId, BigDecimal limit);
}
