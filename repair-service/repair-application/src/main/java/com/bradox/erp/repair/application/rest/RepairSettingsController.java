package com.bradox.erp.repair.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.repair.domain.core.model.Settings;
import com.bradox.erp.repair.service.domain.dto.UpdateSettingsCommand;
import com.bradox.erp.repair.service.domain.ports.input.SettingsApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/repair/settings", produces = "application/json")
public class RepairSettingsController {

    private final SettingsApplicationService settings;

    public RepairSettingsController(SettingsApplicationService settings) {
        this.settings = settings;
    }

    @GetMapping
    public Settings get(@CurrentCompany CompanyId companyId) {
        return settings.get(companyId);
    }

    @PutMapping
    public Settings update(@CurrentCompany CompanyId companyId, @Valid @RequestBody UpdateSettingsCommand command) {
        return settings.update(companyId, command);
    }
}
