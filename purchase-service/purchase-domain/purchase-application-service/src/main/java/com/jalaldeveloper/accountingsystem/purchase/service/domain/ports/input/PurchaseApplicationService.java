package com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement.PartnerStatementSectionResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockPickingResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ValidatePickingCommand;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.PurchaseOrderState;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PurchaseApplicationService {

    PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderCommand command);

    Page<PurchaseOrderSummaryResponse> searchPurchaseOrders(UUID companyId,
                                                             PurchaseOrderState state,
                                                             UUID vendorPartnerId,
                                                             String q,
                                                             Pageable pageable);

    PurchaseOrderResponse getPurchaseOrder(UUID id);

    PurchaseOrderResponse sendPurchaseOrder(UUID id);

    PurchaseOrderResponse confirmPurchaseOrder(UUID id);

    PurchaseOrderResponse cancelPurchaseOrder(UUID id);

    StockPickingResponse validateReceiptPicking(UUID pickingId, ValidatePickingCommand command);

    /**
     * Recomputes each line's {@code qty_received} from done stock moves (same rules as after purchase receipt validate).
     * Called when an incoming picking is validated via inventory so vendor billing sees received quantities.
     */
    void syncPurchaseOrderLineQtyReceivedFromStockMoves(UUID purchaseOrderId);

    VendorBillResponse createVendorBillFromPo(CreateVendorBillFromPoCommand command);

    VendorBillResponse postVendorBill(UUID billId);

    VendorBillResponse getVendorBill(UUID billId);

    List<VendorBillSummaryResponse> listVendorBills(UUID companyId);

    List<VendorPaymentResponse> listVendorPayments(UUID companyId);

    List<PartnerStatementSectionResponse> payableStatement(UUID companyId,
                                                     UUID partnerId,
                                                     LocalDate from,
                                                     LocalDate to);

    VendorPaymentResponse registerVendorPayment(RegisterVendorPaymentCommand command);

    FiscalTaxResponse createFiscalTax(CreateFiscalTaxCommand command);

    List<FiscalTaxResponse> listFiscalTaxes(UUID companyId);

    FiscalTaxResponse getFiscalTax(UUID taxId);
}
