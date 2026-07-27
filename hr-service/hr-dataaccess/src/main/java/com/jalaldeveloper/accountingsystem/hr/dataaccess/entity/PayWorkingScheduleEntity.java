package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pay_working_schedule")
public class PayWorkingScheduleEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "two_week_calendar", nullable = false)
    private boolean twoWeekCalendar;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, dayOfWeek ASC")
    private List<PayWorkingScheduleLineEntity> lines = new ArrayList<>();

    public PayWorkingScheduleEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isTwoWeekCalendar() { return twoWeekCalendar; }
    public void setTwoWeekCalendar(boolean twoWeekCalendar) { this.twoWeekCalendar = twoWeekCalendar; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<PayWorkingScheduleLineEntity> getLines() { return lines; }
    public void setLines(List<PayWorkingScheduleLineEntity> lines) { this.lines = lines; }
}
