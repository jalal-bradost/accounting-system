package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.TimerEntity;
import com.bradox.erp.timesheet.dataaccess.mapper.TimesheetDataAccessMapper;
import com.bradox.erp.timesheet.dataaccess.repository.TimerJpaRepository;
import com.bradox.erp.timesheet.domain.core.entity.Timer;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TimerRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TimerRepositoryImpl implements TimerRepository {

    private final TimerJpaRepository timers;
    private final TimesheetDataAccessMapper mapper;

    public TimerRepositoryImpl(TimerJpaRepository timers, TimesheetDataAccessMapper mapper) {
        this.timers = timers;
        this.mapper = mapper;
    }

    @Override
    public Optional<Timer> find(CompanyId companyId, UUID employeeId) {
        return timers.findByCompanyIdAndEmployeeId(companyId.getId(), employeeId).map(mapper::toDomain);
    }

    @Override
    public Timer save(Timer timer) {
        TimerEntity entity = timers.findById(timer.getId().getId()).orElseGet(TimerEntity::new);
        mapper.apply(timer, entity);
        return mapper.toDomain(timers.saveAndFlush(entity));
    }

    @Override
    public void delete(CompanyId companyId, UUID employeeId) {
        timers.findByCompanyIdAndEmployeeId(companyId.getId(), employeeId).ifPresent(timers::delete);
        timers.flush();
    }
}
