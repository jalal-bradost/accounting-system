package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hr_public_holiday")
public class PublicHolidayEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(nullable = false, length = 128) private String name;
    @Column(name = "holiday_date", nullable = false) private LocalDate holidayDate;
    @Column(name = "country_code", length = 8) private String countryCode;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
