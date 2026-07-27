package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "hr_time_off_type")
public class TimeOffTypeEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(nullable = false, length = 128) private String name;
    @Column(nullable = false, length = 32) private String code;
    @Column(name = "display_code", nullable = false, length = 16) private String displayCode;
    @Column(name = "country_code", length = 8) private String countryCode;
    @Column(name = "color_hex", nullable = false, length = 16) private String colorHex;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean active;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDisplayCode() { return displayCode; }
    public void setDisplayCode(String displayCode) { this.displayCode = displayCode; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
