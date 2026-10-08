package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;

import java.util.Optional;

public interface SettingsRepository {

    Optional<TimesheetSettings> find(CompanyId companyId);

    /** Companies that turned auto-lock on, for the daily job. */
    java.util.List<TimesheetSettings> findAllWithAutoLock();

    /** Companies whose weekly reminder is on, for the daily job. */
    java.util.List<TimesheetSettings> findAllWithReminders();

    /** Companies that post labor cost to the ledger, for the daily integrity check. */
    java.util.List<TimesheetSettings> findAllWithLedgerPosting();

    TimesheetSettings save(TimesheetSettings settings);
}
