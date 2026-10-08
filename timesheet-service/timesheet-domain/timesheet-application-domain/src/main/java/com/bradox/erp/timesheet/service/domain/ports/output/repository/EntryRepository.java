package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.valueobject.EntryId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface EntryRepository {

    record Filter(UUID employeeId, LocalDate from, LocalDate to, UUID projectId, UUID taskId, Boolean billable) {
    }

    Optional<Entry> find(EntryId id);

    /** Newest work date first. */
    List<Entry> search(CompanyId companyId, Filter filter);

    List<Entry> findForEmployee(CompanyId companyId, UUID employeeId, LocalDate from, LocalDate to);

    List<Entry> findForCell(CompanyId companyId, UUID employeeId, LocalDate date, UUID projectId, UUID taskId);

    /** Minutes already logged by the employee on a date, not counting {@code excludeId}. */
    int minutesOnDay(CompanyId companyId, UUID employeeId, LocalDate date, EntryId excludeId);

    Map<UUID, Long> minutesByProject(CompanyId companyId, Collection<UUID> projectIds);

    Map<UUID, Long> minutesByTask(CompanyId companyId, Collection<UUID> taskIds);

    boolean existsForProject(UUID projectId);

    List<Entry> findByWeek(WeekId weekId);

    /** Minutes of billable entries in APPROVED weeks, per sales line (BR-TSH-13). */
    Map<UUID, Long> sumApprovedBillableMinutesByLine(CompanyId companyId, Collection<UUID> lineIds);

    /** Billable entries of APPROVED weeks mapped to the lines, oldest first, for the invoiced allocation (TSH-06 #9). */
    List<Entry> findApprovedBillableByLines(CompanyId companyId, Collection<UUID> lineIds);

    /** Billable entries of APPROVED weeks of hourly projects that have no sales line yet (TSH-06 #6). */
    List<Entry> findApprovedBillableWithoutLine(CompanyId companyId);

    Map<UUID, Long> minutesByWeek(Collection<WeekId> weekIds);

    List<Entry> findByRecord(CompanyId companyId, String recordModel, UUID recordId);

    /** Approved entries whose cost could not be priced (TSH-07 #6). */
    List<Entry> findCostMissing(CompanyId companyId);

    record ProjectTotals(long minutes, long billableMinutes, java.math.BigDecimal costAmount, long missingCostEntries) {
    }

    ProjectTotals projectTotals(CompanyId companyId, UUID projectId);

    Entry save(Entry entry);

    void delete(EntryId id);
}
