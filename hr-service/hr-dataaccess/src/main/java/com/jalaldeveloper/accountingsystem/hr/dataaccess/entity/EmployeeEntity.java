package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ArchivableEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hr_employee")
public class EmployeeEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "work_email", length = 255)
    private String workEmail;

    @Column(name = "work_phone", length = 50)
    private String workPhone;

    @Column(name = "mobile_phone", length = 50)
    private String mobilePhone;

    @Column(name = "job_title", length = 255)
    private String jobTitle;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "work_street", length = 255)
    private String workStreet;

    @Column(name = "work_city", length = 100)
    private String workCity;

    @Column(name = "work_state", length = 100)
    private String workState;

    @Column(name = "work_postal_code", length = 20)
    private String workPostalCode;

    @Column(name = "work_country", length = 100)
    private String workCountry;

    @Column(name = "work_location", length = 255)
    private String workLocation;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    @Column(name = "user_id")
    private UUID userId;

    public EmployeeEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getWorkEmail() { return workEmail; }
    public void setWorkEmail(String workEmail) { this.workEmail = workEmail; }
    public String getWorkPhone() { return workPhone; }
    public void setWorkPhone(String workPhone) { this.workPhone = workPhone; }
    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID managerId) { this.managerId = managerId; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public String getWorkStreet() { return workStreet; }
    public void setWorkStreet(String workStreet) { this.workStreet = workStreet; }
    public String getWorkCity() { return workCity; }
    public void setWorkCity(String workCity) { this.workCity = workCity; }
    public String getWorkState() { return workState; }
    public void setWorkState(String workState) { this.workState = workState; }
    public String getWorkPostalCode() { return workPostalCode; }
    public void setWorkPostalCode(String workPostalCode) { this.workPostalCode = workPostalCode; }
    public String getWorkCountry() { return workCountry; }
    public void setWorkCountry(String workCountry) { this.workCountry = workCountry; }
    public String getWorkLocation() { return workLocation; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String imageContentType) { this.imageContentType = imageContentType; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
