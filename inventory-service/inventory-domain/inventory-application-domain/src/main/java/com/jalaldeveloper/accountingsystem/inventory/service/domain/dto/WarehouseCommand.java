package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class WarehouseCommand {
    private UUID companyId;
    @NotBlank private String code;
    @NotBlank private String name;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
}
