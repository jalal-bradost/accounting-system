package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hr_mandatory_day")
public class MandatoryDayEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(nullable = false, length = 128) private String name;
    @Column(name = "mandatory_date", nullable = false) private LocalDate mandatoryDate;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getMandatoryDate() { return mandatoryDate; }
    public void setMandatoryDate(LocalDate mandatoryDate) { this.mandatoryDate = mandatoryDate; }
}
