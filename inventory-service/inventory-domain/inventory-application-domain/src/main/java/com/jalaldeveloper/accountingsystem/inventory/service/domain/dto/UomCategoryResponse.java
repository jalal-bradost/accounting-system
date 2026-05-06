package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import java.util.UUID;

public class UomCategoryResponse {
    private UUID id;
    private UUID companyId;
    private String name;
    private boolean active;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
}
