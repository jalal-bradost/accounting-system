package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.valueobject.EntryId;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.domain.core.entity.Timer;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort;
import com.bradox.erp.timesheet.domain.core.entity.WeekPosting;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingId;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.service.domain.ports.output.LaborCostPostingPort;
import com.bradox.erp.timesheet.service.domain.ports.output.TimesheetNotificationPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ReminderLogRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineBillingPort;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.PostingRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRateRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.LaborCostPort;
import com.bradox.erp.timesheet.service.domain.ports.output.PartnerLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TimerRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.GridLineRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.SettingsRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class Fakes {

    private Fakes() {
    }

    static class ProjectRepo implements ProjectRepository {
        final Map<ProjectId, Project> store = new LinkedHashMap<>();

        public Optional<Project> find(ProjectId id) { return Optional.ofNullable(store.get(id)); }
        public List<Project> findByIds(Collection<ProjectId> ids) {
            return ids.stream().map(store::get).filter(Objects::nonNull).toList();
        }
        public List<Project> findAll(CompanyId c, boolean includeArchived) {
            return store.values().stream().filter(p -> p.getCompanyId().equals(c))
                    .filter(p -> includeArchived || p.getStatus() == ProjectStatus.ACTIVE).toList();
        }
        public Optional<Project> findBySystemKey(CompanyId c, String key) {
            return store.values().stream().filter(p -> p.getCompanyId().equals(c) && key.equals(p.getSystemKey())).findFirst();
        }
        public boolean codeTaken(CompanyId c, String code, ProjectId exclude) {
            return store.values().stream().anyMatch(p -> p.getCompanyId().equals(c) && !p.getId().equals(exclude)
                    && code.equals(Project.normalizeCode(p.getCode())));
        }
        public Project save(Project p) { store.put(p.getId(), p); return p; }
        public Project createIfAbsent(Project p) {
            return findBySystemKey(p.getCompanyId(), p.getSystemKey()).orElseGet(() -> save(p));
        }
    }

    static class TaskRepo implements TaskRepository {
        final Map<TaskId, Task> store = new LinkedHashMap<>();

        public Optional<Task> find(TaskId id) { return Optional.ofNullable(store.get(id)); }
        public List<Task> findByIds(Collection<TaskId> ids) {
            return ids.stream().map(store::get).filter(Objects::nonNull).toList();
        }
        public List<Task> findByProject(CompanyId c, ProjectId p) {
            return store.values().stream().filter(t -> t.getProjectId().equals(p)).toList();
        }
        public List<Task> findOpenByProjects(CompanyId c, Collection<ProjectId> ids) {
            return store.values().stream().filter(t -> ids.contains(t.getProjectId()) && t.acceptsEntries()).toList();
        }
        public Optional<Task> findByRecord(CompanyId c, String model, UUID recordId) {
            return store.values().stream().filter(t -> model.equals(t.getRecordModel()) && recordId.equals(t.getRecordId())).findFirst();
        }
        public Task save(Task t) { store.put(t.getId(), t); return t; }
    }

    static class EntryRepo implements EntryRepository {
        final Map<EntryId, Entry> store = new LinkedHashMap<>();
        WeekRepo weeks;

        private boolean approved(Entry e) {
            var w = weeks == null ? null : weeks.store.get(e.getWeekId());
            return w != null && w.getStatus() == com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus.APPROVED;
        }

        public Map<UUID, Long> sumApprovedBillableMinutesByLine(CompanyId c, Collection<UUID> ids) {
            return store.values().stream().filter(e -> e.isBillable() && e.getSaleLineId() != null && ids.contains(e.getSaleLineId()) && approved(e))
                    .collect(Collectors.groupingBy(Entry::getSaleLineId, Collectors.summingLong(Entry::getMinutes)));
        }
        public List<Entry> findApprovedBillableByLines(CompanyId c, Collection<UUID> ids) {
            return store.values().stream().filter(e -> e.isBillable() && e.getSaleLineId() != null && ids.contains(e.getSaleLineId()) && approved(e)).toList();
        }
        public List<Entry> findApprovedBillableWithoutLine(CompanyId c) {
            return store.values().stream().filter(e -> e.isBillable() && e.getSaleLineId() == null && approved(e)).toList();
        }

        public Optional<Entry> find(EntryId id) { return Optional.ofNullable(store.get(id)); }
        public List<Entry> search(CompanyId c, Filter f) {
            return store.values().stream().filter(e -> e.getCompanyId().equals(c))
                    .filter(e -> f.employeeId() == null || e.getEmployeeId().equals(f.employeeId()))
                    .filter(e -> f.from() == null || !e.getWorkDate().isBefore(f.from()))
                    .filter(e -> f.to() == null || !e.getWorkDate().isAfter(f.to()))
                    .filter(e -> f.projectId() == null || e.getProjectId().getId().equals(f.projectId()))
                    .filter(e -> f.billable() == null || e.isBillable() == f.billable())
                    .sorted(Comparator.comparing(Entry::getWorkDate).reversed()).toList();
        }
        public List<Entry> findForEmployee(CompanyId c, UUID emp, LocalDate from, LocalDate to) {
            return search(c, new Filter(emp, from, to, null, null, null));
        }
        public List<Entry> findForCell(CompanyId c, UUID emp, LocalDate date, UUID project, UUID task) {
            return store.values().stream().filter(e -> e.getEmployeeId().equals(emp) && e.getWorkDate().equals(date)
                    && e.getProjectId().getId().equals(project)
                    && Objects.equals(e.getTaskId() == null ? null : e.getTaskId().getId(), task)).toList();
        }
        public int minutesOnDay(CompanyId c, UUID emp, LocalDate date, EntryId exclude) {
            return store.values().stream().filter(e -> e.getEmployeeId().equals(emp) && e.getWorkDate().equals(date)
                    && !e.getId().equals(exclude)).mapToInt(Entry::getMinutes).sum();
        }
        public Map<UUID, Long> minutesByProject(CompanyId c, Collection<UUID> ids) {
            return store.values().stream().filter(e -> ids.contains(e.getProjectId().getId()))
                    .collect(Collectors.groupingBy(e -> e.getProjectId().getId(), Collectors.summingLong(Entry::getMinutes)));
        }
        public Map<UUID, Long> minutesByTask(CompanyId c, Collection<UUID> ids) {
            return store.values().stream().filter(e -> e.getTaskId() != null && ids.contains(e.getTaskId().getId()))
                    .collect(Collectors.groupingBy(e -> e.getTaskId().getId(), Collectors.summingLong(Entry::getMinutes)));
        }
        public boolean existsForProject(UUID projectId) {
            return store.values().stream().anyMatch(e -> e.getProjectId().getId().equals(projectId));
        }
        public List<Entry> findByWeek(WeekId weekId) {
            return store.values().stream().filter(e -> e.getWeekId().equals(weekId)).toList();
        }
        public Map<UUID, Long> minutesByWeek(Collection<WeekId> ids) {
            return store.values().stream().filter(e -> ids.contains(e.getWeekId()))
                    .collect(Collectors.groupingBy(e -> e.getWeekId().getId(), Collectors.summingLong(Entry::getMinutes)));
        }
        public List<Entry> findByRecord(CompanyId c, String model, UUID recordId) {
            return store.values().stream().filter(e -> model.equals(e.getRecordModel()) && recordId.equals(e.getRecordId())).toList();
        }
        public List<Entry> findCostMissing(CompanyId c) { return store.values().stream().filter(Entry::isCostMissing).toList(); }
        public ProjectTotals projectTotals(CompanyId c, UUID projectId) {
            var es = store.values().stream().filter(e -> e.getProjectId().getId().equals(projectId)).toList();
            return new ProjectTotals(es.stream().mapToLong(Entry::getMinutes).sum(),
                    es.stream().filter(Entry::isBillable).mapToLong(Entry::getMinutes).sum(),
                    es.stream().map(e -> e.getCostAmount() == null ? java.math.BigDecimal.ZERO : e.getCostAmount())
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                    es.stream().filter(Entry::isCostMissing).count());
        }
        public Entry save(Entry e) { store.put(e.getId(), e); return e; }
        public void delete(EntryId id) { store.remove(id); }
    }

    static class WeekRepo implements WeekRepository {
        final Map<WeekId, TimesheetWeek> store = new LinkedHashMap<>();

        public Optional<TimesheetWeek> find(CompanyId c, UUID emp, LocalDate start) {
            return store.values().stream().filter(w -> w.getEmployeeId().equals(emp) && w.getWeekStart().equals(start)).findFirst();
        }
        public List<TimesheetWeek> findByIds(Collection<WeekId> ids) {
            return ids.stream().map(store::get).filter(Objects::nonNull).toList();
        }
        public Optional<TimesheetWeek> findById(WeekId id) { return Optional.ofNullable(store.get(id)); }
        public List<TimesheetWeek> search(CompanyId c, Collection<WeekStatus> statuses, Collection<UUID> emps, LocalDate from, LocalDate to) {
            return store.values().stream().filter(w -> w.getCompanyId().equals(c))
                    .filter(w -> statuses == null || statuses.contains(w.getStatus()))
                    .filter(w -> emps == null || emps.contains(w.getEmployeeId()))
                    .filter(w -> from == null || !w.getWeekStart().isBefore(from))
                    .filter(w -> to == null || !w.getWeekStart().isAfter(to)).toList();
        }
        public List<TimesheetWeek> findForEmployees(CompanyId c, LocalDate start, Collection<UUID> emps) {
            return store.values().stream().filter(w -> w.getWeekStart().equals(start) && emps.contains(w.getEmployeeId())).toList();
        }
        public int lockStartedBefore(CompanyId c, LocalDate cutoff) {
            int n = 0;
            for (TimesheetWeek w : store.values()) {
                if (!w.isLocked() && w.getWeekStart().isBefore(cutoff)) { w.lock(); n++; }
            }
            return n;
        }
        public TimesheetWeek save(TimesheetWeek w) { store.put(w.getId(), w); return w; }
    }

    static class SettingsRepo implements SettingsRepository {
        TimesheetSettings current;
        public Optional<TimesheetSettings> find(CompanyId c) { return Optional.ofNullable(current); }
        public List<TimesheetSettings> findAllWithLedgerPosting() {
            return current != null && current.isLedgerPostingEnabled() ? List.of(current) : List.of();
        }
        public List<TimesheetSettings> findAllWithReminders() {
            TimesheetSettings s = current != null ? current : TimesheetSettings.defaults(ServiceTestBase.COMPANY);
            return s.isReminderEnabled() ? List.of(s) : List.of();
        }
        public List<TimesheetSettings> findAllWithAutoLock() {
            return current != null && current.getAutoLockAfterDays() != null ? List.of(current) : List.of();
        }
        public TimesheetSettings save(TimesheetSettings s) { current = s; return s; }
    }

    static class LineRepo implements GridLineRepository {
        final List<Line> store = new ArrayList<>();
        public List<Line> list(CompanyId c, UUID emp) { return List.copyOf(store); }
        public void add(CompanyId c, UUID emp, UUID project, UUID task) {
            Line l = new Line(project, task);
            if (!store.contains(l)) store.add(l);
        }
        public void remove(CompanyId c, UUID emp, UUID project, UUID task) { store.remove(new Line(project, task)); }
    }

    static class Employees implements EmployeeLookupPort {
        final Map<UUID, EmployeeRef> byId = new HashMap<>();
        /** Who the fake request is signed in as (user to employee), used to find the caller. */
        final Map<UUID, UUID> byUser = new HashMap<>();
        /** Which platform user belongs to which employee, for notifications. Independent of who is signed in. */
        final Map<UUID, UUID> userOfEmployee = new HashMap<>();

        final Map<UUID, UUID> departmentManagers = new HashMap<>();

        EmployeeRef add(String name, UUID userId) { return add(name, userId, null, null); }

        EmployeeRef add(String name, UUID userId, UUID managerId, UUID departmentId) {
            EmployeeRef e = new EmployeeRef(UUID.randomUUID(), name, managerId, departmentId);
            byId.put(e.id(), e);
            if (userId != null) {
                byUser.put(userId, e.id());
                userOfEmployee.put(e.id(), userId);
            }
            return e;
        }
        public Optional<EmployeeRef> findByUser(CompanyId c, UUID userId) {
            return Optional.ofNullable(byUser.get(userId)).map(byId::get);
        }
        public Optional<EmployeeRef> find(CompanyId c, UUID id) { return Optional.ofNullable(byId.get(id)); }
        public Map<UUID, EmployeeRef> findAll(CompanyId c, Collection<UUID> ids) {
            return ids.stream().filter(byId::containsKey).collect(Collectors.toMap(i -> i, byId::get, (a, b) -> a));
        }
        public Optional<UUID> departmentManager(CompanyId c, UUID departmentId) {
            return Optional.ofNullable(departmentManagers.get(departmentId));
        }
        public Map<UUID, String> departmentNames(CompanyId c, Collection<UUID> ids) {
            return ids.stream().collect(Collectors.toMap(i -> i, i -> "Department"));
        }
        public List<EmployeeRef> findTeam(CompanyId c, UUID managerId) {
            return byId.values().stream().filter(e -> !e.id().equals(managerId)
                    && (managerId.equals(e.managerId()) || (e.departmentId() != null && managerId.equals(departmentManagers.get(e.departmentId()))))).toList();
        }
        public Map<UUID, UUID> userIds(CompanyId c, Collection<UUID> ids) {
            Map<UUID, UUID> out = new HashMap<>();
            userOfEmployee.forEach((emp, user) -> { if (ids.contains(emp)) out.put(emp, user); });
            return out;
        }
        public List<EmployeeRef> listActive(CompanyId c) { return List.copyOf(byId.values()); }
    }

    static class Partners implements PartnerLookupPort {
        final Set<UUID> known = new java.util.HashSet<>();
        public boolean exists(CompanyId c, UUID id) { return known.contains(id); }
        public Map<UUID, String> names(CompanyId c, Collection<UUID> ids) {
            return ids.stream().filter(known::contains).collect(Collectors.toMap(i -> i, i -> "Customer"));
        }
    }

    static class Expected implements ExpectedHoursPort {
        boolean noSchedule;

        public Map<LocalDate, ExpectedDay> expected(CompanyId c, UUID emp, LocalDate from, LocalDate to) {
            Map<LocalDate, ExpectedDay> m = new LinkedHashMap<>();
            if (noSchedule) {
                for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) m.put(d, new ExpectedDay(0, DayType.NO_SCHEDULE, null));
                return m;
            }
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                boolean weekend = d.getDayOfWeek() == java.time.DayOfWeek.FRIDAY || d.getDayOfWeek() == java.time.DayOfWeek.SATURDAY;
                m.put(d, weekend ? new ExpectedDay(0, DayType.NON_WORKING, null) : new ExpectedDay(480, DayType.WORKING, null));
            }
            return m;
        }
    }

    static class Timers implements TimerRepository {
        final Map<UUID, Timer> byEmployee = new HashMap<>();
        public Optional<Timer> find(CompanyId c, UUID emp) { return Optional.ofNullable(byEmployee.get(emp)); }
        public Timer save(Timer t) { byEmployee.put(t.getEmployeeId(), t); return t; }
        public void delete(CompanyId c, UUID emp) { byEmployee.remove(emp); }
    }

    static class Costs implements LaborCostPort {
        java.math.BigDecimal perHour;
        String currency = "IQD";
        public Optional<Rate> hourlyRate(CompanyId c, UUID emp, LocalDate asOf) {
            return perHour == null ? Optional.empty() : Optional.of(new Rate(perHour, currency));
        }
    }

    static class SalesLines implements SalesLineLookupPort, SalesLineBillingPort {
        final Map<UUID, SaleLine> lines = new HashMap<>();
        final List<String> calls = new ArrayList<>();
        RuntimeException failWith;

        SaleLine add(String order, String name, String unit, boolean eligible, boolean closed, double invoiced) {
            SaleLine l = new SaleLine(UUID.randomUUID(), UUID.randomUUID(), order, null, name, unit, eligible, closed,
                    java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(invoiced));
            lines.put(l.lineId(), l);
            return l;
        }
        public Optional<SaleLine> find(CompanyId c, UUID id) { return Optional.ofNullable(lines.get(id)); }
        public Map<UUID, SaleLine> findAll(CompanyId c, Collection<UUID> ids) {
            return ids.stream().filter(lines::containsKey).collect(Collectors.toMap(i -> i, lines::get));
        }
        public List<SaleLine> listEligible(CompanyId c, UUID partner) { return lines.values().stream().filter(SaleLine::eligible).toList(); }
        public void setDelivered(CompanyId c, UUID id, java.math.BigDecimal qty, String source) {
            if (failWith != null) throw failWith;
            SaleLine l = lines.get(id);
            calls.add(id + "=" + qty.toPlainString());
            lines.put(id, new SaleLine(l.lineId(), l.orderId(), l.orderName(), l.customerPartnerId(), l.lineName(), l.unit(),
                    l.eligible(), l.closed(), l.qtyOrdered(), qty, l.qtyInvoiced()));
        }
    }

    static class Rates implements ProjectRateRepository {
        final Map<UUID, Map<UUID, UUID>> byProject = new HashMap<>();
        public Map<UUID, UUID> find(ProjectId p) { return byProject.getOrDefault(p.getId(), Map.of()); }
        public Map<UUID, Map<UUID, UUID>> findForProjects(Collection<ProjectId> ids) {
            Map<UUID, Map<UUID, UUID>> out = new HashMap<>();
            ids.forEach(i -> { if (byProject.containsKey(i.getId())) out.put(i.getId(), byProject.get(i.getId())); });
            return out;
        }
        public void replace(CompanyId c, ProjectId p, Map<UUID, UUID> m) { byProject.put(p.getId(), new java.util.LinkedHashMap<>(m)); }
    }

    static class Postings implements PostingRepository {
        final Map<PostingId, WeekPosting> store = new LinkedHashMap<>();
        WeekRepo weeks;
        public Optional<WeekPosting> find(PostingId id) { return Optional.ofNullable(store.get(id)); }
        public Optional<WeekPosting> findActiveByWeek(WeekId w) { return store.values().stream().filter(p -> p.getWeekId().equals(w) && p.isActive()).findFirst(); }
        public int maxVersion(WeekId w) { return store.values().stream().filter(p -> p.getWeekId().equals(w)).mapToInt(WeekPosting::getVersion).max().orElse(0); }
        public Map<UUID, WeekPosting> findLatestByWeeks(Collection<WeekId> ids) {
            Map<UUID, WeekPosting> out = new HashMap<>();
            store.values().stream().filter(p -> ids.contains(p.getWeekId()))
                    .sorted(Comparator.comparingInt(WeekPosting::getVersion)).forEach(p -> out.put(p.getWeekId().getId(), p));
            return out;
        }
        public List<WeekPosting> search(CompanyId c, Collection<PostingStatus> st) {
            return store.values().stream().filter(p -> st == null || st.isEmpty() || st.contains(p.getStatus())).toList();
        }
        public List<WeekPosting> findRetryable(java.time.Instant stale) {
            return store.values().stream().filter(p -> (p.getStatus() == PostingStatus.FAILED && p.getAttempts() < WeekPosting.MAX_ATTEMPTS)
                    || (p.getStatus() == PostingStatus.PENDING && p.getCreatedAt().isBefore(stale))).toList();
        }
        public List<WeekId> approvedWeeksWithoutActivePosting(CompanyId c) {
            return weeks.store.values().stream().filter(w -> w.getStatus() == com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus.APPROVED
                    && findActiveByWeek(w.getId()).isEmpty()).sorted(Comparator.comparing(TimesheetWeek::getWeekStart))
                    .map(TimesheetWeek::getId).toList();
        }
        public java.math.BigDecimal sumPosted(CompanyId c, LocalDate from, LocalDate to) {
            return store.values().stream().filter(p -> p.getStatus() == PostingStatus.POSTED && p.getEntryDate() != null
                    && !p.getEntryDate().isBefore(from) && !p.getEntryDate().isAfter(to))
                    .map(WeekPosting::getTotalAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        }
        public Optional<WeekPosting> findByJournalEntry(CompanyId c, UUID je) {
            return store.values().stream().filter(p -> je.equals(p.getJournalEntryId()) || je.equals(p.getReversalEntryId())).findFirst();
        }
        public WeekPosting save(WeekPosting p) { store.put(p.getId(), p); return p; }
    }

    static class Ledger implements LaborCostPostingPort {
        final List<Request> posted = new ArrayList<>();
        final List<UUID> reversed = new ArrayList<>();
        RuntimeException failWith;
        boolean periodClosedForWeekEnd;
        String base = "IQD";
        java.math.BigDecimal rate = java.math.BigDecimal.ONE;
        final UUID costAccount = UUID.randomUUID();
        final UUID appliedAccount = UUID.randomUUID();

        public Result post(Request r) {
            if (failWith != null) throw failWith;
            posted.add(r);
            boolean late = periodClosedForWeekEnd;
            return new Result(UUID.randomUUID(), late ? r.fallbackDate() : r.entryDate(), late);
        }
        public UUID reverse(CompanyId c, UUID je, String reason) {
            if (failWith != null) throw failWith;
            reversed.add(je);
            return UUID.randomUUID();
        }
        public Defaults ensureDefaults(CompanyId c, String journal) { return new Defaults(costAccount, appliedAccount); }
        public String baseCurrency(CompanyId c) { return base; }
        java.math.BigDecimal salary = java.math.BigDecimal.ZERO;
        public java.math.BigDecimal salaryExpense(CompanyId c, LocalDate from, LocalDate to) { return salary; }
        public java.math.BigDecimal toBaseCurrency(CompanyId c, java.math.BigDecimal amount, String cur, LocalDate asOf) {
            return amount.multiply(rate);
        }
    }

    static class Notifications implements TimesheetNotificationPort {
        record Todo(String model, UUID record, String assignee, String subject, String body) {
        }
        final List<Todo> todos = new ArrayList<>();
        final List<String> completed = new ArrayList<>();
        final List<String> approvers = new ArrayList<>();
        RuntimeException failWith;

        public void assignTodo(CompanyId c, String model, UUID record, String assignee, String subject, String body, LocalDate due) {
            if (failWith != null) throw failWith;
            todos.add(new Todo(model, record, assignee, subject, body));
        }
        final List<Todo> informed = new ArrayList<>();
        public void inform(CompanyId c, String model, UUID record, String assignee, String subject, String body) {
            if (failWith != null) throw failWith;
            informed.add(new Todo(model, record, assignee, subject, body));
        }
        public void completeTodos(CompanyId c, String model, UUID record) {
            if (failWith != null) throw failWith;
            completed.add(model + ":" + record);
        }
        public List<String> approverAssignees(CompanyId c) { return approvers; }
        List<Todo> to(UUID user) { return todos.stream().filter(t -> t.assignee().equals(user.toString())).toList(); }
    }

    static class ReminderLog implements ReminderLogRepository {
        final java.util.Set<String> keys = new java.util.HashSet<>();
        public boolean exists(UUID e, LocalDate week, String kind) { return keys.contains(e + "|" + week + "|" + kind); }
        public void record(CompanyId c, UUID e, LocalDate week, String kind) { keys.add(e + "|" + week + "|" + kind); }
    }

    static class Attendance implements com.bradox.erp.timesheet.service.domain.ports.output.AttendanceLookupPort {
        final Map<LocalDate, Integer> minutes = new HashMap<>();
        public Map<LocalDate, Integer> attendedMinutes(CompanyId c, UUID emp, LocalDate from, LocalDate to, java.time.ZoneId zone) {
            Map<LocalDate, Integer> out = new HashMap<>();
            minutes.forEach((d, m) -> { if (!d.isBefore(from) && !d.isAfter(to)) out.put(d, m); });
            return out;
        }
    }
}
