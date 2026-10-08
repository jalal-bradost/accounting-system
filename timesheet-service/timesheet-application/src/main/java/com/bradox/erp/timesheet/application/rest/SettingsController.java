package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.service.domain.dto.EmployeeRefResponse;
import com.bradox.erp.timesheet.service.domain.dto.MeResponse;
import com.bradox.erp.timesheet.service.domain.dto.SettingsCommand;
import com.bradox.erp.timesheet.service.domain.dto.SettingsResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.SettingsApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/timesheet", produces = "application/json")
public class SettingsController {

    private final SettingsApplicationService settings;

    public SettingsController(SettingsApplicationService settings) {
        this.settings = settings;
    }

    @GetMapping("/me")
    @RequiresPermission("tsh.entry.own")
    public MeResponse me(@CurrentCompany CompanyId companyId) {
        return settings.me(companyId);
    }

    @GetMapping("/employees")
    @RequiresPermission("tsh.entry.own")
    public List<EmployeeRefResponse> employees(@CurrentCompany CompanyId companyId) {
        return settings.employees(companyId);
    }

    @GetMapping("/settings")
    @RequiresPermission("tsh.entry.own")
    public SettingsResponse get(@CurrentCompany CompanyId companyId) {
        return settings.get(companyId);
    }

    @PutMapping("/settings")
    @RequiresPermission("tsh.settings.manage")
    public SettingsResponse update(@CurrentCompany CompanyId companyId, @RequestBody SettingsCommand command) {
        return settings.update(companyId, command);
    }
}
