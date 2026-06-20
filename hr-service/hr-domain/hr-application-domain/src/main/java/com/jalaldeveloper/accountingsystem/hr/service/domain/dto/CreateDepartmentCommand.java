package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class CreateDepartmentCommand {
    private UUID companyId;
    @NotBlank private String name;
    private UUID parentId;
    private UUID managerId;
    private int colorIndex;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID v) { this.parentId = v; }
    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID v) { this.managerId = v; }
    public int getColorIndex() { return colorIndex; }
    public void setColorIndex(int v) { this.colorIndex = v; }
}
