package com.bradox.erp.timesheet.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "tsh_project_rate")
public class ProjectRateEntity {

    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    @Column(name = "sale_line_id", nullable = false)
    private UUID saleLineId;

    public ProjectRateEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getSaleLineId() { return saleLineId; }
    public void setSaleLineId(UUID saleLineId) { this.saleLineId = saleLineId; }
}
