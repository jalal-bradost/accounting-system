package com.bradox.delin.purchase.service.domain.ports.input;

import com.bradox.delin.accounting.service.domain.partnerstatement.PartnerStatementSectionResponse;
import com.bradox.delin.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.delin.inventory.service.domain.dto.ValidatePickingCommand;
import com.bradox.delin.purchase.domain.core.PurchaseOrderState;
import com.bradox.delin.purchase.service.domain.dto.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    VendorBillResponse getVendorBill(UUID billId);

    List<VendorBillSummaryResponse> listVendorBills(UUID companyId);

    List<VendorPaymentResponse> listVendorPayments(UUID companyId);

    List<PartnerStatementSectionResponse> payableStatement(UUID companyId,
                                                     UUID partnerId,
                                                     LocalDate from,
                                                     LocalDate to);

    VendorPaymentResponse registerVendorPayment(RegisterVendorPaymentCommand command);

    VendorPaymentResponse reverseVendorPayment(UUID paymentId, String reason);

    FiscalTaxResponse createFiscalTax(CreateFiscalTaxCommand command);

    List<FiscalTaxResponse> listFiscalTaxes(UUID companyId);

    FiscalTaxResponse getFiscalTax(UUID taxId);
}
