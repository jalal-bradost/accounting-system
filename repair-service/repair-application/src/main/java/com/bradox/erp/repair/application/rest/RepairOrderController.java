package com.bradox.erp.repair.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.repair.service.domain.dto.RepairOrderCommand;
import com.bradox.erp.repair.service.domain.dto.RepairOrderResponse;
import com.bradox.erp.repair.service.domain.ports.input.RepairOrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/repair/orders", produces = "application/json")
public class RepairOrderController {

    private final RepairOrderApplicationService orders;

    public RepairOrderController(RepairOrderApplicationService orders) {
        this.orders = orders;
    }

    @GetMapping
    public List<RepairOrderResponse> list(@CurrentCompany CompanyId companyId) {
        return orders.list(companyId);
    }

    @GetMapping("/{id}")
    public RepairOrderResponse get(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return orders.get(companyId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepairOrderResponse create(@CurrentCompany CompanyId companyId, @Valid @RequestBody RepairOrderCommand c) {
        return orders.create(companyId, c);
    }

    @PutMapping("/{id}")
    public RepairOrderResponse update(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody RepairOrderCommand c) {
        return orders.update(companyId, id, c);
    }

    @PostMapping("/{id}/confirm")
    public RepairOrderResponse confirm(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return orders.confirm(companyId, id);
    }

    @PostMapping("/{id}/done")
    public RepairOrderResponse done(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return orders.done(companyId, id);
    }

    @PostMapping("/{id}/cancel")
    public RepairOrderResponse cancel(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return orders.cancel(companyId, id);
    }
}
