package com.jalaldeveloper.accountingsystem.purchase.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockPickingResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ValidatePickingCommand;
import com.jalaldeveloper.accountingsystem.platform.application.dto.PageResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.PurchaseOrderState;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.*;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.input.PurchaseApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/purchase", produces = "application/json")
public class PurchaseController {

    private final PurchaseApplicationService purchaseApplicationService;

    public PurchaseController(PurchaseApplicationService purchaseApplicationService) {
        this.purchaseApplicationService = purchaseApplicationService;
    }

    @PostMapping("/orders")
    @RequiresPermission("purchase.order.write")
    public ResponseEntity<PurchaseOrderResponse> createOrder(@CurrentCompany CompanyId companyId,
                                                             @Valid @RequestBody CreatePurchaseOrderCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.createPurchaseOrder(cmd));
    }

    @GetMapping("/orders")
    @RequiresPermission("purchase.order.read")
    public ResponseEntity<PageResponse<PurchaseOrderSummaryResponse>> listOrders(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) UUID vendorPartnerId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PurchaseOrderState stateFilter = null;
        if (state != null && !state.isBlank()) {
            stateFilter = PurchaseOrderState.valueOf(state.trim().toUpperCase(Locale.ROOT));
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = purchaseApplicationService.searchPurchaseOrders(
                companyId.getId(), stateFilter, vendorPartnerId, q, pageable);
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }

    @GetMapping("/orders/{id}")
    @RequiresPermission("purchase.order.read")
    public ResponseEntity<PurchaseOrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.getPurchaseOrder(id));
    }

    @PostMapping("/orders/{id}/send")
    @RequiresPermission("purchase.order.confirm")
    public ResponseEntity<PurchaseOrderResponse> send(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.sendPurchaseOrder(id));
    }

    @PostMapping("/orders/{id}/confirm")
    @RequiresPermission("purchase.order.confirm")
    public ResponseEntity<PurchaseOrderResponse> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.confirmPurchaseOrder(id));
    }

    @PostMapping("/orders/{id}/cancel")
    @RequiresPermission("purchase.order.write")
    public ResponseEntity<PurchaseOrderResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.cancelPurchaseOrder(id));
    }

    @PostMapping("/receipts/{pickingId}/validate")
    @RequiresPermission("purchase.receipt.validate")
    public ResponseEntity<StockPickingResponse> validateReceipt(@PathVariable UUID pickingId,
                                                                @RequestBody(required = false) ValidatePickingCommand body) {
        return ResponseEntity.ok(purchaseApplicationService.validateReceiptPicking(pickingId, body));
    }

    @PostMapping("/vendor-bills/from-po")
    @RequiresPermission("purchase.vendor-bill.write")
    public ResponseEntity<VendorBillResponse> createBillFromPo(@CurrentCompany CompanyId companyId,
                                                               @Valid @RequestBody CreateVendorBillFromPoCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.createVendorBillFromPo(cmd));
    }

    @GetMapping("/vendor-bills")
    @RequiresPermission("purchase.vendor-bill.read")
    public ResponseEntity<List<VendorBillSummaryResponse>> listVendorBills(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(purchaseApplicationService.listVendorBills(companyId.getId()));
    }

    @GetMapping("/vendor-bills/{id}")
    @RequiresPermission("purchase.vendor-bill.read")
    public ResponseEntity<VendorBillResponse> getVendorBill(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.getVendorBill(id));
    }

    @GetMapping("/vendor-bills/{id}/credit-notes")
    @RequiresPermission("purchase.vendor-bill.read")
    public ResponseEntity<List<VendorBillResponse>> listCreditNotes(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.listCreditNotesForBill(id));
    }

    @PostMapping("/vendor-bills/{id}/credit-note")
    @RequiresPermission("purchase.vendor-bill.write")
    public ResponseEntity<VendorBillResponse> createCreditNote(@CurrentCompany CompanyId companyId,
                                                               @PathVariable UUID id,
                                                               @Valid @RequestBody CreateCreditNoteFromVendorBillCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.createCreditNoteFromVendorBill(id, cmd));
    }

    @PostMapping("/vendor-bills/{id}/post")
    @RequiresPermission("purchase.vendor-bill.post")
    public ResponseEntity<VendorBillResponse> postBill(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.postVendorBill(id));
    }

    @GetMapping("/vendor-payments")
    @RequiresPermission("purchase.vendor-bill.read")
    public ResponseEntity<List<VendorPaymentResponse>> listVendorPayments(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(purchaseApplicationService.listVendorPayments(companyId.getId()));
    }

    @PostMapping("/vendor-payments")
    @RequiresPermission("purchase.payment.register")
    public ResponseEntity<VendorPaymentResponse> registerPayment(@CurrentCompany CompanyId companyId,
                                                                 @Valid @RequestBody RegisterVendorPaymentCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.registerVendorPayment(cmd));
    }

    @PostMapping("/fiscal-taxes")
    @RequiresPermission("purchase.fiscal-tax.write")
    public ResponseEntity<FiscalTaxResponse> createTax(@CurrentCompany CompanyId companyId,
                                                       @Valid @RequestBody CreateFiscalTaxCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.createFiscalTax(cmd));
    }

    @GetMapping("/fiscal-taxes")
    @RequiresPermission("purchase.fiscal-tax.read")
    public ResponseEntity<List<FiscalTaxResponse>> listTaxes(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(purchaseApplicationService.listFiscalTaxes(companyId.getId()));
    }
}
