package com.bradox.delin.contacts.application.rest;

import com.bradox.delin.contacts.service.domain.dto.PaymentTermsCommand;
import com.bradox.delin.contacts.service.domain.dto.PaymentTermsResponse;
import com.bradox.delin.contacts.service.domain.ports.input.PaymentTermsApplicationService;
import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.platform.security.RequiresPermission;
import com.bradox.delin.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/contacts/payment-terms", produces = "application/json")
public class PaymentTermsController {

    private final PaymentTermsApplicationService service;

    public PaymentTermsController(PaymentTermsApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("contacts.payment-terms.write")
    public ResponseEntity<PaymentTermsResponse> create(@Valid @RequestBody PaymentTermsCommand cmd) {
        return ResponseEntity.ok(service.create(cmd));
    }

    @GetMapping("/{id}")
    @RequiresPermission("contacts.payment-terms.read")
    public ResponseEntity<PaymentTermsResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequiresPermission("contacts.payment-terms.write")
    public ResponseEntity<PaymentTermsResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody PaymentTermsCommand cmd) {
        return ResponseEntity.ok(service.update(id, cmd));
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("contacts.payment-terms.write")
    public ResponseEntity<PaymentTermsResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(service.archive(id));
    }

    @PostMapping("/{id}/unarchive")
    @RequiresPermission("contacts.payment-terms.write")
    public ResponseEntity<PaymentTermsResponse> unarchive(@PathVariable UUID id) {
        return ResponseEntity.ok(service.unarchive(id));
    }

    @GetMapping
    @RequiresPermission("contacts.payment-terms.read")
    public ResponseEntity<List<PaymentTermsResponse>> list(@CurrentCompany CompanyId companyId,
                                                           @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(service.list(companyId, includeArchived));
    }
}
