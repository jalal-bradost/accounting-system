package com.bradox.erp.inventory.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.service.domain.dto.OpeningStockCommand;
import com.bradox.erp.inventory.service.domain.dto.OpeningStockReport;
import com.bradox.erp.inventory.service.domain.ports.input.OpeningStockApplicationService;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/inventory/opening-stock", produces = "application/json")
public class OpeningStockController {

    private final OpeningStockApplicationService service;

    public OpeningStockController(OpeningStockApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("inventory.picking.write")
    public ResponseEntity<OpeningStockReport> importOpeningStock(
            @CurrentCompany CompanyId companyId,
            @Valid @RequestBody OpeningStockCommand command) {
        if (command.getCompanyId() == null) {
            command.setCompanyId(companyId.getId());
        }
        return ResponseEntity.ok(service.importOpeningStock(command));
    }
}
