package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ArchivableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "hr_department")
public class DepartmentEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "color_index", nullable = false)
    private int colorIndex;

    public DepartmentEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID managerId) { this.managerId = managerId; }
    public int getColorIndex() { return colorIndex; }
    public void setColorIndex(int colorIndex) { this.colorIndex = colorIndex; }
}
