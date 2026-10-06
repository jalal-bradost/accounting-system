package com.bradox.erp.purchase.service.domain.ports.input;

import com.bradox.erp.accounting.service.domain.partnerstatement.PartnerStatementSectionResponse;
import com.bradox.erp.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.erp.inventory.service.domain.dto.ValidatePickingCommand;
import com.bradox.erp.purchase.domain.core.PurchaseOrderState;
import com.bradox.erp.purchase.service.domain.dto.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PurchaseApplicationService {

    PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderCommand command);

    PurchaseOrderResponse updatePurchaseOrder(UUID id, CreatePurchaseOrderCommand command);

    Page<PurchaseOrderSummaryResponse> searchPurchaseOrders(UUID companyId,
                                                             PurchaseOrderState state,
                                                             UUID vendorPartnerId,
                                                             String q,
                                                             Pageable pageable);

    PurchaseOrderResponse getPurchaseOrder(UUID id);

    PurchaseOrderResponse sendPurchaseOrder(UUID id);

    PurchaseOrderResponse confirmPurchaseOrder(UUID id);

    PurchaseOrderResponse cancelPurchaseOrder(UUID id);

    /**
     * Guided return to the vendor in one transaction: return pickings across the order's receipts,
     * validated at the original cost; refund credits and posts, replace re-receives.
     */
    PurchaseOrderResponse returnGoods(UUID id, com.bradox.erp.purchase.service.domain.dto.ReturnGoodsCommand command);

    /** Price, discount, tax or order-discount change after receipt/billing: credit the bills, update, rebill. */
    com.bradox.erp.purchase.service.domain.dto.PurchaseCorrectionResult changeTerms(
            UUID id, com.bradox.erp.purchase.service.domain.dto.PurchaseCorrectionCommand command);

    /** Lower quantities after billing: credit the billed-but-unreceived part, reduce the order and open receipts. */
    com.bradox.erp.purchase.service.domain.dto.PurchaseCorrectionResult reduceQuantities(
            UUID id, com.bradox.erp.purchase.service.domain.dto.PurchaseCorrectionCommand command);

    /** Return everything received, credit everything billed, cancel open documents and the order. */
    com.bradox.erp.purchase.service.domain.dto.PurchaseCorrectionResult cancelWithDocuments(
            UUID id, com.bradox.erp.purchase.service.domain.dto.PurchaseCorrectionCommand command);

    /** Wrong vendor before receipt: credit the bills, change the vendor, rebill. */
    com.bradox.erp.purchase.service.domain.dto.PurchaseCorrectionResult reassignVendor(
            UUID id, com.bradox.erp.purchase.service.domain.dto.PurchaseCorrectionCommand command);

    /** Vendor won't ship the rest: ordered = received on every goods line, open receipts cancelled. */
    PurchaseOrderResponse closeRemainingQuantities(UUID id, String reason);

    PurchaseOrderResponse lockPurchaseOrder(UUID id);

    PurchaseOrderResponse unlockPurchaseOrder(UUID id);

    StockPickingResponse validateReceiptPicking(UUID pickingId, ValidatePickingCommand command);

    /**
     * Creates a draft return picking from the latest DONE receipt for the purchase order.
     * Caller validates the return (and can adjust qty) so a credit note can follow.
     */
    StockPickingResponse createReturnFromPurchaseOrder(UUID purchaseOrderId);

    StockPickingResponse createReturnFromPurchaseOrder(UUID purchaseOrderId, CreatePurchaseReturnCommand command);

    /**
     * Recomputes each line's {@code qty_received} from done stock moves (same rules as after purchase receipt validate).
     * Called when an incoming picking is validated via inventory so vendor billing sees received quantities.
     */
    void syncPurchaseOrderLineQtyReceivedFromStockMoves(UUID purchaseOrderId, UUID pickingId);

    VendorBillResponse createVendorBillFromPo(CreateVendorBillFromPoCommand command);

    VendorBillResponse updateVendorBill(UUID id, UpdateVendorBillCommand command);

    VendorBillResponse cancelVendorBill(UUID id);

    VendorBillResponse createCreditNoteFromVendorBill(UUID billId, CreateCreditNoteFromVendorBillCommand command);

    VendorBillResponse createDebitNoteFromVendorBill(UUID billId, CreateDebitNoteFromVendorBillCommand command);

    List<VendorBillResponse> listCreditNotesForBill(UUID billId);

    VendorBillResponse postVendorBill(UUID billId);

    /**
     * Creates and posts an opening-balance vendor bill on the OPEN journal
     * (Dr Opening Balance Equity / Cr AP partner). Sequence {@code OB-BILL}.
     */
    VendorBillResponse createOpeningVendorBill(UUID companyId, UUID partnerId, BigDecimal amount, String currency,
                                               LocalDate date, LocalDate dueDate, String reference,
                                               UUID openingJournalId, UUID openingEquityAccountId);

    /** Reverse the JE and set CANCELLED on an opening vendor bill. */
    void cancelOpeningVendorBill(UUID billId);

    VendorPaymentResponse postOpeningVendorPayment(UUID companyId, UUID partnerId, BigDecimal amount, String currency,
                                                   LocalDate date, String reference,
                                                   UUID openingJournalId, UUID openingEquityAccountId);

    List<VendorBillResponse> listOpeningVendorBills(UUID companyId);

    List<VendorPaymentResponse> listOpeningVendorPayments(UUID companyId);

    boolean hasActiveVendorAllocations(java.util.Collection<UUID> billIds, java.util.Collection<UUID> paymentIds);

    boolean hasVendorCreditNotes(java.util.Collection<UUID> billIds);

    VendorBillResponse getVendorBill(UUID billId);

    List<VendorBillSummaryResponse> listVendorBills(UUID companyId);

    Page<VendorBillSummaryResponse> searchVendorBills(UUID companyId, Pageable pageable);

    Page<VendorPaymentResponse> searchVendorPayments(UUID companyId, Pageable pageable);

    List<VendorPaymentResponse> listVendorPayments(UUID companyId);

    VendorPaymentResponse getVendorPayment(UUID paymentId);

    List<PartnerStatementSectionResponse> payableStatement(UUID companyId,
                                                     UUID partnerId,
                                                     LocalDate from,
                                                     LocalDate to);

    VendorPaymentResponse registerVendorPayment(RegisterVendorPaymentCommand command);

    VendorPaymentResponse allocateVendorPayment(UUID paymentId, AllocateVendorPaymentCommand command);

    VendorPaymentResponse deallocateVendorPayment(UUID allocationId);

    VendorPaymentResponse reverseVendorPayment(UUID paymentId, String reason);

    /** Reverse and re-register a wrong payment in one step. */
    VendorPaymentResponse correctVendorPayment(UUID paymentId,
            com.bradox.erp.purchase.service.domain.dto.CorrectVendorPaymentCommand command);

    /** Collect our credit on a paid, credited bill back from the vendor. */
    VendorPaymentResponse refundVendorCredit(UUID billId,
            com.bradox.erp.purchase.service.domain.dto.RefundVendorCreditCommand command);

    /** Release our credit on a bill so it becomes an open payment for the vendor's next bill. Returns the amount. */
    BigDecimal keepVendorCredit(UUID billId);

    /** Settle a posted bill with the vendor's open payments. Returns the amount applied. */
    BigDecimal applyVendorCredit(UUID billId);

    FiscalTaxResponse createFiscalTax(CreateFiscalTaxCommand command);

    List<FiscalTaxResponse> listFiscalTaxes(UUID companyId);

    FiscalTaxResponse getFiscalTax(UUID taxId);
}
