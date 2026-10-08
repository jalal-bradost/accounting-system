package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.ReminderLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface ReminderLogJpaRepository extends JpaRepository<ReminderLogEntity, UUID> {

    boolean existsByEmployeeIdAndRunWeekAndKind(UUID employeeId, LocalDate runWeek, String kind);
}
