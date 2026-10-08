package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeekRepository {

    Optional<TimesheetWeek> find(CompanyId companyId, UUID employeeId, LocalDate weekStart);

    List<TimesheetWeek> findByIds(Collection<WeekId> ids);

    Optional<TimesheetWeek> findById(WeekId id);

    /** Weeks in the given statuses; {@code employeeIds} null means everyone. Newest week first. */
    List<TimesheetWeek> search(CompanyId companyId, Collection<WeekStatus> statuses, Collection<UUID> employeeIds,
                               LocalDate from, LocalDate to);

    /** The weeks that start on {@code weekStart}, for the given employees. */
    List<TimesheetWeek> findForEmployees(CompanyId companyId, LocalDate weekStart, Collection<UUID> employeeIds);

    /** TSH-05 #9: locks every unlocked week that started before the cutoff. Returns how many were locked. */
    int lockStartedBefore(CompanyId companyId, LocalDate cutoff);

    TimesheetWeek save(TimesheetWeek week);
}
