package com.bradox.erp.purchase.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.erp.inventory.service.domain.dto.ValidatePickingCommand;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.purchase.domain.core.PurchaseOrderState;
import com.bradox.erp.purchase.service.domain.PurchaseDashboardService;
import com.bradox.erp.purchase.service.domain.dto.*;
import com.bradox.erp.purchase.service.domain.ports.input.PurchaseApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/purchase", produces = "application/json")
public class PurchaseController {

    private final PurchaseApplicationService purchaseApplicationService;
    private final PurchaseDashboardService purchaseDashboardService;

    public PurchaseController(PurchaseApplicationService purchaseApplicationService,
                              PurchaseDashboardService purchaseDashboardService) {
        this.purchaseApplicationService = purchaseApplicationService;
        this.purchaseDashboardService = purchaseDashboardService;
    }

    @GetMapping("/dashboard")
    @RequiresPermission("purchase.order.read")
    public ResponseEntity<PurchaseDashboardResponse> dashboard(
            @CurrentCompany CompanyId companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(purchaseDashboardService.getDashboard(companyId.getId(), from, to));
    }

    @PostMapping("/orders")
    @RequiresPermission("purchase.order.write")
    public ResponseEntity<PurchaseOrderResponse> createOrder(@CurrentCompany CompanyId companyId,
                                                             @Valid @RequestBody CreatePurchaseOrderCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.createPurchaseOrder(cmd));
    }

    @PutMapping("/orders/{id}")
    @RequiresPermission("purchase.order.write")
    public ResponseEntity<PurchaseOrderResponse> updateOrder(@CurrentCompany CompanyId companyId,
                                                             @PathVariable UUID id,
                                                             @Valid @RequestBody CreatePurchaseOrderCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.updatePurchaseOrder(id, cmd));
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

    @PostMapping("/orders/{id}/lock")
    @RequiresPermission("purchase.order.write")
    public ResponseEntity<PurchaseOrderResponse> lock(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.lockPurchaseOrder(id));
    }

    @PostMapping("/orders/{id}/unlock")
    @RequiresPermission("purchase.order.write")
    public ResponseEntity<PurchaseOrderResponse> unlock(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.unlockPurchaseOrder(id));
    }

    @PostMapping("/orders/{id}/return")
    @RequiresPermission("purchase.receipt.validate")
    public ResponseEntity<StockPickingResponse> createReturn(@PathVariable UUID id,
                                                             @RequestBody(required = false) CreatePurchaseReturnCommand body) {
        return ResponseEntity.ok(purchaseApplicationService.createReturnFromPurchaseOrder(
                id, body != null ? body : new CreatePurchaseReturnCommand()));
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

    @PutMapping("/vendor-bills/{id}")
    @RequiresPermission("purchase.vendor-bill.write")
    public ResponseEntity<VendorBillResponse> updateVendorBill(@PathVariable UUID id,
                                                               @Valid @RequestBody UpdateVendorBillCommand cmd) {
        return ResponseEntity.ok(purchaseApplicationService.updateVendorBill(id, cmd));
    }

    @PostMapping("/vendor-bills/{id}/cancel")
    @RequiresPermission("purchase.vendor-bill.write")
    public ResponseEntity<VendorBillResponse> cancelVendorBill(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.cancelVendorBill(id));
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

    @PostMapping("/vendor-bills/{id}/debit-note")
    @RequiresPermission("purchase.vendor-bill.write")
    public ResponseEntity<VendorBillResponse> createDebitNote(@CurrentCompany CompanyId companyId,
                                                              @PathVariable UUID id,
                                                              @Valid @RequestBody CreateDebitNoteFromVendorBillCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.createDebitNoteFromVendorBill(id, cmd));
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
