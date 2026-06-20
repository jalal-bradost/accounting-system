package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import java.util.UUID;

public class UpdateDepartmentCommand {
    private String name;
    private UUID parentId;
    private Boolean parentIdReset;
    private UUID managerId;
    private Boolean managerIdReset;
    private Integer colorIndex;

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID v) { this.parentId = v; }
    public Boolean getParentIdReset() { return parentIdReset; }
    public void setParentIdReset(Boolean v) { this.parentIdReset = v; }
    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID v) { this.managerId = v; }
    public Boolean getManagerIdReset() { return managerIdReset; }
    public void setManagerIdReset(Boolean v) { this.managerIdReset = v; }
    public Integer getColorIndex() { return colorIndex; }
    public void setColorIndex(Integer v) { this.colorIndex = v; }
}
