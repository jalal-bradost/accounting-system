package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.Timer;

import java.util.Optional;
import java.util.UUID;

public interface TimerRepository {

    Optional<Timer> find(CompanyId companyId, UUID employeeId);

    Timer save(Timer timer);

    void delete(CompanyId companyId, UUID employeeId);
}
