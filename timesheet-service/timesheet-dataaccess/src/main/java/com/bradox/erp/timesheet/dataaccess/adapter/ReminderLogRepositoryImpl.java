package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.ReminderLogEntity;
import com.bradox.erp.timesheet.dataaccess.repository.ReminderLogJpaRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ReminderLogRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class ReminderLogRepositoryImpl implements ReminderLogRepository {

    private final ReminderLogJpaRepository log;

    public ReminderLogRepositoryImpl(ReminderLogJpaRepository log) {
        this.log = log;
    }

    @Override
    public boolean exists(UUID employeeId, LocalDate runWeek, String kind) {
        return log.existsByEmployeeIdAndRunWeekAndKind(employeeId, runWeek, kind);
    }

    @Override
    public void record(CompanyId companyId, UUID employeeId, LocalDate runWeek, String kind) {
        if (exists(employeeId, runWeek, kind)) {
            return;
        }
        ReminderLogEntity e = new ReminderLogEntity();
        e.setId(UUID.randomUUID());
        e.setCompanyId(companyId.getId());
        e.setEmployeeId(employeeId);
        e.setRunWeek(runWeek);
        e.setKind(kind);
        e.setCreatedAt(Instant.now());
        log.saveAndFlush(e);
    }
}
