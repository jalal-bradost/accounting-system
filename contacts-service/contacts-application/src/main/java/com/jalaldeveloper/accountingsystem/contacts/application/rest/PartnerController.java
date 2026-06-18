package com.jalaldeveloper.accountingsystem.contacts.application.rest;

import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.*;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PartnerApplicationService;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.application.dto.PageResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/contacts/partners", produces = "application/json")
public class PartnerController {

    private final PartnerApplicationService service;

    public PartnerController(PartnerApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("contacts.partner.write")
    public ResponseEntity<PartnerResponse> create(@Valid @RequestBody CreatePartnerCommand cmd) {
        return ResponseEntity.ok(service.createPartner(cmd));
    }

    @GetMapping("/{id}")
    @RequiresPermission("contacts.partner.read")
    public ResponseEntity<PartnerResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getPartner(id));
    }

    @PutMapping("/{id}")
    @RequiresPermission("contacts.partner.write")
    public ResponseEntity<PartnerResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdatePartnerCommand cmd) {
        return ResponseEntity.ok(service.updatePartner(id, cmd));
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("contacts.partner.archive")
    public ResponseEntity<PartnerResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(service.archive(id));
    }

    @PostMapping("/{id}/unarchive")
    @RequiresPermission("contacts.partner.archive")
    public ResponseEntity<PartnerResponse> unarchive(@PathVariable UUID id) {
        return ResponseEntity.ok(service.unarchive(id));
    }

    @GetMapping
    @RequiresPermission("contacts.partner.read")
    public ResponseEntity<PageResponse<PartnerResponse>> list(@CurrentCompany CompanyId companyId,
                                                              @RequestParam(required = false) String q,
                                                              @RequestParam(required = false) Boolean isCustomer,
                                                              @RequestParam(required = false) Boolean isVendor,
                                                              @RequestParam(defaultValue = "false") boolean includeArchived,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "50") int size) {
        var result = service.search(companyId, q, isCustomer, isVendor, includeArchived, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }

    @PostMapping("/{id}/addresses")
    @RequiresPermission("contacts.partner.write")
    public ResponseEntity<PartnerResponse.AddressResponse> addAddress(@PathVariable UUID id,
                                                                       @Valid @RequestBody PartnerAddressCommand cmd) {
        return ResponseEntity.ok(service.addAddress(id, cmd));
    }

    @PutMapping("/{id}/addresses/{addressId}")
    @RequiresPermission("contacts.partner.write")
    public ResponseEntity<PartnerResponse.AddressResponse> updateAddress(@PathVariable UUID id,
                                                                         @PathVariable UUID addressId,
                                                                         @Valid @RequestBody PartnerAddressCommand cmd) {
        return ResponseEntity.ok(service.updateAddress(id, addressId, cmd));
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    @RequiresPermission("contacts.partner.write")
    public ResponseEntity<Void> removeAddress(@PathVariable UUID id, @PathVariable UUID addressId) {
        service.removeAddress(id, addressId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/bank-accounts")
    @RequiresPermission("contacts.partner.write")
    public ResponseEntity<PartnerResponse.BankAccountResponse> addBankAccount(@PathVariable UUID id,
                                                                               @Valid @RequestBody PartnerBankAccountCommand cmd) {
        return ResponseEntity.ok(service.addBankAccount(id, cmd));
    }

    @DeleteMapping("/{id}/bank-accounts/{bankAccountId}")
    @RequiresPermission("contacts.partner.write")
    public ResponseEntity<Void> removeBankAccount(@PathVariable UUID id, @PathVariable UUID bankAccountId) {
        service.removeBankAccount(id, bankAccountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/credit-status")
    @RequiresPermission("contacts.partner.read")
    public ResponseEntity<CreditStatusResponse> creditStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(service.creditStatus(id));
    }

    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    @RequiresPermission("contacts.partner.write")
    public ResponseEntity<PartnerResponse> uploadImage(@PathVariable UUID id,
                                                       @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(service.uploadPartnerImage(id, file));
    }

    @DeleteMapping("/{id}/image")
    @RequiresPermission("contacts.partner.write")
    public ResponseEntity<PartnerResponse> deleteImage(@PathVariable UUID id) {
        return ResponseEntity.ok(service.deletePartnerImage(id));
    }
}
