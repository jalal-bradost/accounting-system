package com.bradox.erp.repair.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.repair.domain.core.model.ServicePackage;
import com.bradox.erp.repair.service.domain.dto.PackageCommand;
import com.bradox.erp.repair.service.domain.ports.input.PackageApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/repair/packages", produces = "application/json")
public class PackageController {

    private final PackageApplicationService packages;

    public PackageController(PackageApplicationService packages) {
        this.packages = packages;
    }

    @GetMapping
    public List<ServicePackage> list(@CurrentCompany CompanyId companyId, @RequestParam(required = false) String q,
                                     @RequestParam(defaultValue = "false") boolean includeArchived) {
        return packages.list(companyId, q, includeArchived);
    }

    @GetMapping("/{id}")
    public ServicePackage get(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return packages.get(companyId, id);
    }

    @PostMapping
    public ServicePackage create(@CurrentCompany CompanyId companyId, @Valid @RequestBody PackageCommand command) {
        return packages.save(companyId, null, command);
    }

    @PutMapping("/{id}")
    public ServicePackage update(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody PackageCommand command) {
        return packages.save(companyId, id, command);
    }

    @PostMapping("/{id}/archive")
    public ServicePackage archive(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return packages.setActive(companyId, id, false);
    }

    @PostMapping("/{id}/restore")
    public ServicePackage restore(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return packages.setActive(companyId, id, true);
    }
}
