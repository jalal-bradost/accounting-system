package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.EntryEntity;
import com.bradox.erp.timesheet.dataaccess.mapper.TimesheetDataAccessMapper;
import com.bradox.erp.timesheet.dataaccess.repository.EntryJpaRepository;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.valueobject.EntryId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class EntryRepositoryImpl implements EntryRepository {

    private static final UUID NOTHING_EXCLUDED = new UUID(0L, 0L);

    private final EntryJpaRepository entries;
    private final TimesheetDataAccessMapper mapper;

    public EntryRepositoryImpl(EntryJpaRepository entries, TimesheetDataAccessMapper mapper) {
        this.entries = entries;
        this.mapper = mapper;
    }

    @Override
    public Optional<Entry> find(EntryId id) {
        return entries.findById(id.getId()).map(mapper::toDomain);
    }

    @Override
    public List<Entry> search(CompanyId companyId, Filter f) {
        Specification<EntryEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("companyId"), companyId.getId()));
            if (f.employeeId() != null) p.add(cb.equal(root.get("employeeId"), f.employeeId()));
            if (f.from() != null) p.add(cb.greaterThanOrEqualTo(root.get("workDate"), f.from()));
            if (f.to() != null) p.add(cb.lessThanOrEqualTo(root.get("workDate"), f.to()));
            if (f.projectId() != null) p.add(cb.equal(root.get("projectId"), f.projectId()));
            if (f.taskId() != null) p.add(cb.equal(root.get("taskId"), f.taskId()));
            if (f.billable() != null) p.add(cb.equal(root.get("billable"), f.billable()));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return entries.findAll(spec, Sort.by(Sort.Order.desc("workDate"), Sort.Order.desc("createdAt"))).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Entry> findForEmployee(CompanyId companyId, UUID employeeId, LocalDate from, LocalDate to) {
        return entries.findByCompanyIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAscCreatedAtAsc(
                companyId.getId(), employeeId, from, to).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Entry> findForCell(CompanyId companyId, UUID employeeId, LocalDate date, UUID projectId, UUID taskId) {
        List<EntryEntity> rows = taskId == null
                ? entries.findByCompanyIdAndEmployeeIdAndWorkDateAndProjectIdAndTaskIdIsNull(
                        companyId.getId(), employeeId, date, projectId)
                : entries.findByCompanyIdAndEmployeeIdAndWorkDateAndProjectIdAndTaskId(
                        companyId.getId(), employeeId, date, projectId, taskId);
        return rows.stream().map(mapper::toDomain).toList();
    }

    @Override
    public int minutesOnDay(CompanyId companyId, UUID employeeId, LocalDate date, EntryId excludeId) {
        return (int) entries.minutesOnDay(companyId.getId(), employeeId, date,
                excludeId == null ? NOTHING_EXCLUDED : excludeId.getId());
    }

    @Override
    public Map<UUID, Long> minutesByProject(CompanyId companyId, Collection<UUID> projectIds) {
        return totals(projectIds.isEmpty() ? List.of() : entries.minutesByProject(companyId.getId(), projectIds));
    }

    @Override
    public Map<UUID, Long> minutesByTask(CompanyId companyId, Collection<UUID> taskIds) {
        return totals(taskIds.isEmpty() ? List.of() : entries.minutesByTask(companyId.getId(), taskIds));
    }

    private static Map<UUID, Long> totals(List<Object[]> rows) {
        Map<UUID, Long> out = new HashMap<>();
        for (Object[] r : rows) {
            out.put((UUID) r[0], ((Number) r[1]).longValue());
        }
        return out;
    }

    @Override
    public boolean existsForProject(UUID projectId) {
        return entries.existsByProjectId(projectId);
    }

    @Override
    public List<Entry> findByWeek(WeekId weekId) {
        return entries.findByWeekId(weekId.getId()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Map<UUID, Long> minutesByWeek(Collection<WeekId> weekIds) {
        return weekIds.isEmpty() ? new HashMap<>()
                : totals(entries.minutesByWeek(weekIds.stream().map(WeekId::getId).toList()));
    }

    @Override
    public Map<UUID, Long> sumApprovedBillableMinutesByLine(CompanyId companyId, Collection<UUID> lineIds) {
        return lineIds.isEmpty() ? new HashMap<>() : totals(entries.sumApprovedBillableByLine(companyId.getId(), lineIds));
    }

    @Override
    public List<Entry> findApprovedBillableByLines(CompanyId companyId, Collection<UUID> lineIds) {
        return lineIds.isEmpty() ? List.of()
                : entries.findApprovedBillableByLines(companyId.getId(), lineIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Entry> findApprovedBillableWithoutLine(CompanyId companyId) {
        return entries.findApprovedBillableWithoutLine(companyId.getId()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Entry> findByRecord(CompanyId companyId, String recordModel, UUID recordId) {
        return entries.findByCompanyIdAndRecordModelAndRecordIdOrderByWorkDateDesc(companyId.getId(), recordModel, recordId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Entry> findCostMissing(CompanyId companyId) {
        return entries.findByCompanyIdAndCostMissingTrue(companyId.getId()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ProjectTotals projectTotals(CompanyId companyId, UUID projectId) {
        Object[] r = entries.projectTotals(companyId.getId(), projectId).get(0);
        return new ProjectTotals(((Number) r[0]).longValue(), ((Number) r[1]).longValue(),
                r[2] instanceof java.math.BigDecimal b ? b : java.math.BigDecimal.valueOf(((Number) r[2]).doubleValue()),
                ((Number) r[3]).longValue());
    }

    @Override
    public Entry save(Entry entry) {
        EntryEntity entity = entries.findById(entry.getId().getId()).orElseGet(EntryEntity::new);
        mapper.apply(entry, entity);
        return mapper.toDomain(entries.saveAndFlush(entity));
    }

    @Override
    public void delete(EntryId id) {
        entries.deleteById(id.getId());
        entries.flush();
    }
}
