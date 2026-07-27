package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pay_structure")
public class PayStructureEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "structure_type_id")
    private UUID structureTypeId;

    @Column(name = "scheduled_pay", nullable = false, length = 32)
    private String scheduledPay;

    @Column(name = "use_worked_day_lines", nullable = false)
    private boolean useWorkedDayLines;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "structure", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<PaySalaryRuleEntity> rules = new ArrayList<>();

    public PayStructureEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getStructureTypeId() { return structureTypeId; }
    public void setStructureTypeId(UUID structureTypeId) { this.structureTypeId = structureTypeId; }
    public String getScheduledPay() { return scheduledPay; }
    public void setScheduledPay(String scheduledPay) { this.scheduledPay = scheduledPay; }
    public boolean isUseWorkedDayLines() { return useWorkedDayLines; }
    public void setUseWorkedDayLines(boolean useWorkedDayLines) { this.useWorkedDayLines = useWorkedDayLines; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<PaySalaryRuleEntity> getRules() { return rules; }
    public void setRules(List<PaySalaryRuleEntity> rules) { this.rules = rules; }
}
