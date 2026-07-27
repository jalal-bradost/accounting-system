package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pay_working_schedule_line")
public class PayWorkingScheduleLineEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PayWorkingScheduleEntity schedule;

    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal hours;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public PayWorkingScheduleLineEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PayWorkingScheduleEntity getSchedule() { return schedule; }
    public void setSchedule(PayWorkingScheduleEntity schedule) { this.schedule = schedule; }
    public short getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(short dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
