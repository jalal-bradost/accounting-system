package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.SettingsEntity;
import com.bradox.erp.timesheet.dataaccess.mapper.TimesheetDataAccessMapper;
import com.bradox.erp.timesheet.dataaccess.repository.SettingsJpaRepository;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.SettingsRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SettingsRepositoryImpl implements SettingsRepository {

    private final SettingsJpaRepository settings;
    private final TimesheetDataAccessMapper mapper;

    public SettingsRepositoryImpl(SettingsJpaRepository settings, TimesheetDataAccessMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
    }

    @Override
    public Optional<TimesheetSettings> find(CompanyId companyId) {
        return settings.findByCompanyId(companyId.getId()).map(mapper::toDomain);
    }

    @Override
    public java.util.List<TimesheetSettings> findAllWithAutoLock() {
        return settings.findByAutoLockAfterDaysNotNull().stream().map(mapper::toDomain).toList();
    }

    @Override
    public java.util.List<TimesheetSettings> findAllWithReminders() {
        return settings.findByReminderEnabledTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public java.util.List<TimesheetSettings> findAllWithLedgerPosting() {
        return settings.findByLedgerPostingEnabledTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public TimesheetSettings save(TimesheetSettings s) {
        SettingsEntity entity = settings.findByCompanyId(s.getCompanyId().getId()).orElseGet(() -> {
            SettingsEntity fresh = new SettingsEntity();
            fresh.setId(UUID.randomUUID());
            return fresh;
        });
        mapper.apply(s, entity);
        return mapper.toDomain(settings.saveAndFlush(entity));
    }
}
