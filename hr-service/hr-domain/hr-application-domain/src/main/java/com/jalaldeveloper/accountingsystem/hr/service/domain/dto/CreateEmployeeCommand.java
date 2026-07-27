package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public class CreateEmployeeCommand {
    private UUID companyId;
    @NotBlank private String displayName;
    private String workEmail;
    private String workPhone;
    private String mobilePhone;
    private String jobTitle;
    private UUID departmentId;
    private UUID managerId;
    private UUID userId;
    private LocalDate hireDate;
    private String workStreet;
    private String workCity;
    private String workState;
    private String workPostalCode;
    private String workCountry;
    private String workLocation;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }
    public String getWorkEmail() { return workEmail; }
    public void setWorkEmail(String v) { this.workEmail = v; }
    public String getWorkPhone() { return workPhone; }
    public void setWorkPhone(String v) { this.workPhone = v; }
    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String v) { this.mobilePhone = v; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String v) { this.jobTitle = v; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID v) { this.departmentId = v; }
    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID v) { this.managerId = v; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate v) { this.hireDate = v; }
    public String getWorkStreet() { return workStreet; }
    public void setWorkStreet(String v) { this.workStreet = v; }
    public String getWorkCity() { return workCity; }
    public void setWorkCity(String v) { this.workCity = v; }
    public String getWorkState() { return workState; }
    public void setWorkState(String v) { this.workState = v; }
    public String getWorkPostalCode() { return workPostalCode; }
    public void setWorkPostalCode(String v) { this.workPostalCode = v; }
    public String getWorkCountry() { return workCountry; }
    public void setWorkCountry(String v) { this.workCountry = v; }
    public String getWorkLocation() { return workLocation; }
    public void setWorkLocation(String v) { this.workLocation = v; }
}
