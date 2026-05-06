package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class UomCategoryCommand {
    private UUID companyId;
    @NotBlank private String name;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
}
