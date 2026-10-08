package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.LocalDate;
import java.util.UUID;

/** Remembers who was reminded in which week, so a reminder goes out once per week per person (TSH-05 #8). */
public interface ReminderLogRepository {

    String EMPLOYEE = "EMPLOYEE";
    String MANAGER = "MANAGER";

    boolean exists(UUID employeeId, LocalDate runWeek, String kind);

    void record(CompanyId companyId, UUID employeeId, LocalDate runWeek, String kind);
}
