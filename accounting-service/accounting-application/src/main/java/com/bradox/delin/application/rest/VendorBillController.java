package com.bradox.delin.application.rest;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.platform.security.RequiresPermission;
import com.bradox.delin.platform.web.CurrentCompany;
import com.bradox.delin.purchase.service.domain.dto.CreateCreditNoteFromVendorBillCommand;
import com.bradox.delin.purchase.service.domain.dto.CreateDebitNoteFromVendorBillCommand;
import com.bradox.delin.purchase.service.domain.dto.CreateVendorBillFromPoCommand;
import com.bradox.delin.purchase.service.domain.dto.UpdateVendorBillCommand;
import com.bradox.delin.purchase.service.domain.dto.VendorBillResponse;
import com.bradox.delin.purchase.service.domain.dto.VendorBillSummaryResponse;
import com.bradox.delin.purchase.service.domain.ports.input.PurchaseApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/accounting/vendor-bills", produces = "application/json")
public class VendorBillController {

    private final PurchaseApplicationService purchaseApplicationService;

    public VendorBillController(PurchaseApplicationService purchaseApplicationService) {
        this.purchaseApplicationService = purchaseApplicationService;
    }

    @PostMapping("/from-po")
    @RequiresPermission("accounting.vendor-bill.write")
    public ResponseEntity<VendorBillResponse> createBillFromPo(@CurrentCompany CompanyId companyId,
                                                               @Valid @RequestBody CreateVendorBillFromPoCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.createVendorBillFromPo(cmd));
    }

    @GetMapping
    @RequiresPermission("accounting.vendor-bill.read")
    public ResponseEntity<List<VendorBillSummaryResponse>> list(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(purchaseApplicationService.listVendorBills(companyId.getId()));
    }

    @GetMapping("/{id}")
    @RequiresPermission("accounting.vendor-bill.read")
    public ResponseEntity<VendorBillResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.getVendorBill(id));
    }

    @PutMapping("/{id}")
    @RequiresPermission("accounting.vendor-bill.write")
    public ResponseEntity<VendorBillResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateVendorBillCommand cmd) {
        return ResponseEntity.ok(purchaseApplicationService.updateVendorBill(id, cmd));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("accounting.vendor-bill.write")
    public ResponseEntity<VendorBillResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.cancelVendorBill(id));
    }

    @GetMapping("/{id}/credit-notes")
    @RequiresPermission("accounting.vendor-bill.read")
    public ResponseEntity<List<VendorBillResponse>> listCreditNotes(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.listCreditNotesForBill(id));
    }

    @PostMapping("/{id}/credit-note")
    @RequiresPermission("accounting.vendor-bill.write")
    public ResponseEntity<VendorBillResponse> createCreditNote(@CurrentCompany CompanyId companyId,
                                                               @PathVariable UUID id,
                                                               @Valid @RequestBody CreateCreditNoteFromVendorBillCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.createCreditNoteFromVendorBill(id, cmd));
    }

    @PostMapping("/{id}/debit-note")
    @RequiresPermission("accounting.vendor-bill.write")
    public ResponseEntity<VendorBillResponse> createDebitNote(@CurrentCompany CompanyId companyId,
                                                              @PathVariable UUID id,
                                                              @Valid @RequestBody CreateDebitNoteFromVendorBillCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(purchaseApplicationService.createDebitNoteFromVendorBill(id, cmd));
    }

    @PostMapping("/{id}/post")
    @RequiresPermission("accounting.vendor-bill.post")
    public ResponseEntity<VendorBillResponse> post(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseApplicationService.postVendorBill(id));
    }
}
