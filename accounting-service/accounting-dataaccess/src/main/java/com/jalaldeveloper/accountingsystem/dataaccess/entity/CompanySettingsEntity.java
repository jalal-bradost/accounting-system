package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "company_settings")
public class CompanySettingsEntity {
    @Id
    @Column(name = "company_id")
    private UUID companyId;
    @Column(name = "period_lock_date")
    private LocalDate periodLockDate;

    public CompanySettingsEntity() {}
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public LocalDate getPeriodLockDate() { return periodLockDate; }
    public void setPeriodLockDate(LocalDate periodLockDate) { this.periodLockDate = periodLockDate; }
}
