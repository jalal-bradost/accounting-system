package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.rule.DurationParser;
import com.bradox.erp.timesheet.domain.core.rule.WeekCalendar;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.CopyLinesCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridCellCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridCellResponse;
import com.bradox.erp.timesheet.service.domain.dto.GridCellResult;
import com.bradox.erp.timesheet.service.domain.dto.GridDayResponse;
import com.bradox.erp.timesheet.service.domain.dto.GridLineCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridResponse;
import com.bradox.erp.timesheet.service.domain.dto.GridRowResponse;
import com.bradox.erp.timesheet.service.domain.dto.TeamGridResponse;
import com.bradox.erp.timesheet.service.domain.dto.FillAttendanceCommand;
import com.bradox.erp.timesheet.service.domain.dto.FillAttendanceResponse;
import com.bradox.erp.timesheet.service.domain.ports.output.AttendanceLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.input.GridApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort.ExpectedDay;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.GridLineRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.GridLineRepository.Line;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
class GridApplicationServiceImpl implements GridApplicationService {

    private final EntryRepository entries;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final WeekRepository weeks;
    private final GridLineRepository lines;
    private final ExpectedHoursPort expectedHours;
    private final EntryWriter writer;
    private final TimesheetAccess access;
    private final EmployeeLookupPort employees;
    private final AttendanceLookupPort attendance;

    GridApplicationServiceImpl(EntryRepository entries, ProjectRepository projects, TaskRepository tasks,
                               WeekRepository weeks, GridLineRepository lines, ExpectedHoursPort expectedHours,
                               EntryWriter writer, TimesheetAccess access, EmployeeLookupPort employees,
                               AttendanceLookupPort attendance) {
        this.entries = entries;
        this.projects = projects;
        this.tasks = tasks;
        this.weeks = weeks;
        this.lines = lines;
        this.expectedHours = expectedHours;
        this.writer = writer;
        this.access = access;
        this.employees = employees;
        this.attendance = attendance;
    }

    @Override
    @Transactional(readOnly = true)
    public GridResponse grid(CompanyId companyId, UUID employeeId, LocalDate date) {
        TimesheetSettings settings = access.settings(companyId);
        EmployeeRef employee = access.resolveForRead(companyId, employeeId);
        LocalDate start = WeekCalendar.weekStart(date == null ? access.today(settings) : date,
                settings.getWeekStartDay());
        List<LocalDate> dates = WeekCalendar.weekDates(start);
        LocalDate end = dates.get(6);

        List<Entry> weekEntries = entries.findForEmployee(companyId, employee.id(), start, end);
        Set<Line> pinned = new LinkedHashSet<>(lines.list(companyId, employee.id()));
        Set<Line> rowKeys = new LinkedHashSet<>(pinned);
        weekEntries.forEach(e -> rowKeys.add(lineOf(e)));

        Map<ProjectId, Project> projectById = projects.findByIds(
                rowKeys.stream().map(l -> new ProjectId(l.projectId())).distinct().toList()).stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));
        Map<TaskId, Task> taskById = tasks.findByIds(rowKeys.stream().map(Line::taskId).filter(Objects::nonNull)
                .map(TaskId::new).distinct().toList()).stream().collect(Collectors.toMap(Task::getId, Function.identity()));

        Map<Line, Map<LocalDate, List<Entry>>> byCell = new HashMap<>();
        for (Entry e : weekEntries) {
            byCell.computeIfAbsent(lineOf(e), k -> new HashMap<>()).computeIfAbsent(e.getWorkDate(), k -> new ArrayList<>()).add(e);
        }

        List<GridRowResponse> rows = rowKeys.stream().map(key -> {
            Project p = projectById.get(new ProjectId(key.projectId()));
            Task t = key.taskId() == null ? null : taskById.get(new TaskId(key.taskId()));
            Map<LocalDate, List<Entry>> cellEntries = byCell.getOrDefault(key, Map.of());
            List<GridCellResponse> cells = dates.stream().map(d -> {
                List<Entry> in = cellEntries.getOrDefault(d, List.of());
                return new GridCellResponse(d, in.stream().mapToInt(Entry::getMinutes).sum(), in.size());
            }).toList();
            boolean open = p != null && p.acceptsEntries() && (t == null || t.acceptsEntries());
            return new GridRowResponse(key.projectId(), p == null ? "?" : p.getName(), p == null ? null : p.getCode(),
                    key.taskId(), t == null ? null : t.getName(), p != null && p.acceptsEntries(), open,
                    pinned.contains(key), cells, cells.stream().mapToInt(GridCellResponse::minutes).sum());
        }).sorted(Comparator.comparing((GridRowResponse r) -> r.projectName().toLowerCase())
                .thenComparing(r -> r.taskName() == null ? "" : r.taskName().toLowerCase())).toList();

        Map<LocalDate, ExpectedDay> expected = expectedHours.expected(companyId, employee.id(), start, end);
        // TSH-03 #11: presence is a hint only. It is never added to the logged time (BR-TSH-18).
        Map<LocalDate, Integer> attended = attendance.attendedMinutes(companyId, employee.id(), start, end, settings.getZone());
        List<GridDayResponse> days = dates.stream().map(d -> {
            ExpectedDay ex = expected.get(d);
            int logged = rows.stream().flatMap(r -> r.cells().stream()).filter(c -> c.date().equals(d))
                    .mapToInt(GridCellResponse::minutes).sum();
            return new GridDayResponse(d, ex == null ? 0 : ex.minutes(),
                    ex == null ? ExpectedHoursPort.DayType.NO_SCHEDULE : ex.type(), ex == null ? null : ex.label(),
                    logged, attended.getOrDefault(d, 0));
        }).toList();

        TimesheetWeek week = weeks.find(companyId, employee.id(), start).orElse(null);
        boolean weekOpen = week == null || week.isEditable();
        boolean mayEdit = weekOpen && access.canModifyFor(companyId, employee.id());
        return new GridResponse(employee.id(), employee.name(), start, end,
                week == null ? WeekStatus.DRAFT : week.getStatus(), week != null && week.isLocked(),
                mayEdit, days, rows,
                days.stream().mapToInt(GridDayResponse::loggedMinutes).sum(),
                days.stream().mapToInt(GridDayResponse::expectedMinutes).sum(),
                week == null ? null : week.getId().getId(), week == null ? null : week.getRefusedReason(),
                mayEdit && !weekEntries.isEmpty(),
                week != null && week.getStatus() == WeekStatus.APPROVED && access.can(TimesheetPermissions.REOPEN));
    }

    @Override
    @Transactional(readOnly = true)
    public TeamGridResponse teamGrid(CompanyId companyId, LocalDate date, boolean all) {
        TimesheetSettings settings = access.settings(companyId);
        List<EmployeeRef> people;
        if (all) {
            access.require(TimesheetPermissions.ENTRY_VIEW_ALL);
            people = employees.listActive(companyId);
        } else {
            if (!access.can(TimesheetPermissions.ENTRY_VIEW_TEAM) && !access.can(TimesheetPermissions.ENTRY_VIEW_ALL)) {
                access.require(TimesheetPermissions.ENTRY_VIEW_TEAM);
            }
            Set<UUID> team = access.teamIds(companyId);
            people = employees.findAll(companyId, team).values().stream().toList();
        }
        people = people.stream().sorted(Comparator.comparing(p -> p.name().toLowerCase())).toList();
        LocalDate start = WeekCalendar.weekStart(date == null ? access.today(settings) : date, settings.getWeekStartDay());
        List<LocalDate> dates = WeekCalendar.weekDates(start);
        LocalDate end = dates.get(6);
        Map<UUID, TimesheetWeek> weekByEmployee = weeks.findForEmployees(companyId, start,
                people.stream().map(EmployeeRef::id).toList()).stream()
                .collect(Collectors.toMap(TimesheetWeek::getEmployeeId, Function.identity(), (a, b) -> a));

        List<TeamGridResponse.TeamEmployee> out = new ArrayList<>();
        Map<ProjectId, String> projectNames = new HashMap<>();
        Map<TaskId, String> taskNames = new HashMap<>();
        for (EmployeeRef person : people) {
            List<Entry> es = entries.findForEmployee(companyId, person.id(), start, end);
            Map<LocalDate, ExpectedDay> expected = expectedHours.expected(companyId, person.id(), start, end);
            Map<Line, int[]> byRow = new LinkedHashMap<>();
            for (Entry e : es) {
                int[] cells = byRow.computeIfAbsent(lineOf(e), k -> new int[7]);
                cells[(int) java.time.temporal.ChronoUnit.DAYS.between(start, e.getWorkDate())] += e.getMinutes();
            }
            projects.findByIds(byRow.keySet().stream().map(l -> new ProjectId(l.projectId())).distinct()
                    .filter(id -> !projectNames.containsKey(id)).toList()).forEach(p -> projectNames.put(p.getId(), p.getName()));
            tasks.findByIds(byRow.keySet().stream().map(Line::taskId).filter(Objects::nonNull).map(TaskId::new).distinct()
                    .filter(id -> !taskNames.containsKey(id)).toList()).forEach(t -> taskNames.put(t.getId(), t.getName()));
            List<TeamGridResponse.TeamRow> rows = byRow.entrySet().stream().map(en -> {
                Line l = en.getKey();
                List<Integer> mins = java.util.Arrays.stream(en.getValue()).boxed().toList();
                return new TeamGridResponse.TeamRow(l.projectId(), projectNames.getOrDefault(new ProjectId(l.projectId()), "?"),
                        l.taskId(), l.taskId() == null ? null : taskNames.get(new TaskId(l.taskId())), mins,
                        mins.stream().mapToInt(Integer::intValue).sum());
            }).sorted(Comparator.comparing((TeamGridResponse.TeamRow r) -> r.projectName().toLowerCase())).toList();
            List<TeamGridResponse.TeamDay> days = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                final int idx = i;
                ExpectedDay ex = expected.get(dates.get(i));
                days.add(new TeamGridResponse.TeamDay(dates.get(i), rows.stream().mapToInt(r -> r.minutes().get(idx)).sum(),
                        ex == null ? 0 : ex.minutes(), ex == null ? ExpectedHoursPort.DayType.NO_SCHEDULE : ex.type()));
            }
            TimesheetWeek w = weekByEmployee.get(person.id());
            out.add(new TeamGridResponse.TeamEmployee(person.id(), person.name(), w == null ? null : w.getId().getId(),
                    w == null ? WeekStatus.DRAFT : w.getStatus(), w != null && w.isLocked(),
                    days.stream().mapToInt(TeamGridResponse.TeamDay::loggedMinutes).sum(),
                    days.stream().mapToInt(TeamGridResponse.TeamDay::expectedMinutes).sum(), days, rows));
        }
        return new TeamGridResponse(start, end, dates, out);
    }

    @Override
    @Transactional
    public GridCellResult setCell(CompanyId companyId, GridCellCommand c) {
        var target = access.resolveForWrite(companyId, c.employeeId());
        UUID employeeId = target.employee().id();
        int minutes = c.duration() != null ? DurationParser.parse(c.duration()) : c.minutes() == null ? 0 : c.minutes();
        if (minutes < 0) {
            throw new TimesheetDomainException("error.timesheet.minutesRange", null,
                    "An entry must be between 1 minute and 24 hours");
        }
        List<Entry> cell = entries.findForCell(companyId, employeeId, c.workDate(), c.projectId(), c.taskId());
        if (cell.size() > 1) {
            throw new TimesheetDomainException("error.timesheet.cellMultiple", null,
                    "This cell holds several entries. Open them to change them one by one.");
        }
        String warning = null;
        int resulting = 0;
        int count = 0;
        if (cell.isEmpty()) {
            if (minutes > 0) {
                EntryWriter.Saved saved = writer.create(companyId, target, c.workDate(), minutes, c.projectId(),
                        c.taskId(), null, null);
                warning = saved.warning();
                resulting = minutes;
                count = 1;
            }
        } else {
            Entry existing = cell.get(0);
            access.requireModifiable(companyId, existing.getEmployeeId());
            if (minutes == 0) {
                writer.delete(companyId, existing);
            } else {
                EntryWriter.Saved saved = writer.update(companyId, existing, existing.getWorkDate(), minutes,
                        c.projectId(), c.taskId(), existing.getDescription(), null);
                warning = saved.warning();
                resulting = minutes;
                count = 1;
            }
        }
        return new GridCellResult(c.workDate(), resulting, count,
                entries.minutesOnDay(companyId, employeeId, c.workDate(), null), warning);
    }

    @Override
    @Transactional
    public FillAttendanceResponse fillFromAttendance(CompanyId companyId, FillAttendanceCommand c) {
        var target = access.resolveForWrite(companyId, c.employeeId());
        TimesheetSettings settings = access.settings(companyId);
        UUID employeeId = target.employee().id();
        LocalDate start = WeekCalendar.weekStart(c.date(), settings.getWeekStartDay());
        LocalDate end = start.plusDays(6);
        Map<LocalDate, Integer> attended = attendance.attendedMinutes(companyId, employeeId, start, end, settings.getZone());
        Map<LocalDate, ExpectedDay> expected = expectedHours.expected(companyId, employeeId, start, end);
        List<LocalDate> filled = new ArrayList<>();
        int total = 0;
        for (LocalDate d : WeekCalendar.weekDates(start)) {
            int present = attended.getOrDefault(d, 0);
            ExpectedDay ex = expected.get(d);
            ExpectedHoursPort.DayType type = ex == null ? ExpectedHoursPort.DayType.NO_SCHEDULE : ex.type();
            boolean offDay = type == ExpectedHoursPort.DayType.NON_WORKING || type == ExpectedHoursPort.DayType.HOLIDAY
                    || type == ExpectedHoursPort.DayType.TIME_OFF;
            if (present <= 0 || offDay || d.isAfter(access.today(settings))
                    || entries.minutesOnDay(companyId, employeeId, d, null) > 0) {
                continue;   // only days with attendance and no logged time yet, on working days (TSH-03 #12)
            }
            // Capped at the expected hours: presence beyond them is overtime a person must log themselves.
            int minutes = type == ExpectedHoursPort.DayType.WORKING ? Math.min(present, ex.minutes()) : Math.min(present, 1440);
            if (minutes < 1) {
                continue;
            }
            writer.create(companyId, target, d, minutes, c.projectId(), c.taskId(), "Filled from attendance", null);
            filled.add(d);
            total += minutes;
        }
        return new FillAttendanceResponse(filled.size(), total, filled);
    }

    @Override
    @Transactional
    public void addLine(CompanyId companyId, GridLineCommand c) {
        var target = access.resolveForWrite(companyId, c.employeeId());
        Project project = projects.find(new ProjectId(c.projectId()))
                .filter(p -> p.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        if (!project.acceptsEntries()) {
            throw new TimesheetDomainException("error.timesheet.projectClosed", null,
                    "This project is archived or does not allow timesheets");
        }
        if (c.taskId() != null) {
            Task task = tasks.find(new TaskId(c.taskId())).filter(t -> t.getCompanyId().equals(companyId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
            if (!task.getProjectId().equals(project.getId())) {
                throw new TimesheetDomainException("error.timesheet.taskProjectMismatch", null,
                        "That task belongs to a different project");
            }
            if (!task.acceptsEntries()) {
                throw new TimesheetDomainException("error.timesheet.taskClosed", null,
                        "This task is done or canceled and takes no new time");
            }
        }
        lines.add(companyId, target.employee().id(), c.projectId(), c.taskId());
    }

    @Override
    @Transactional
    public void removeLine(CompanyId companyId, GridLineCommand c) {
        var target = access.resolveForWrite(companyId, c.employeeId());
        lines.remove(companyId, target.employee().id(), c.projectId(), c.taskId());
    }

    @Override
    @Transactional
    public int copyLastWeekLines(CompanyId companyId, CopyLinesCommand c) {
        var target = access.resolveForWrite(companyId, c.employeeId());
        UUID employeeId = target.employee().id();
        LocalDate start = WeekCalendar.weekStart(c.weekStart(), access.settings(companyId).getWeekStartDay());
        LocalDate prev = start.minusDays(7);
        Set<Line> already = new LinkedHashSet<>(lines.list(companyId, employeeId));
        Set<Line> candidates = entries.findForEmployee(companyId, employeeId, prev, prev.plusDays(6)).stream()
                .map(GridApplicationServiceImpl::lineOf).collect(Collectors.toCollection(LinkedHashSet::new));
        int added = 0;
        for (Line line : candidates) {
            if (already.contains(line) || !stillOpen(companyId, line)) {
                continue;
            }
            lines.add(companyId, employeeId, line.projectId(), line.taskId());
            added++;
        }
        return added;
    }

    private boolean stillOpen(CompanyId companyId, Line line) {
        boolean projectOpen = projects.find(new ProjectId(line.projectId()))
                .filter(p -> p.getCompanyId().equals(companyId)).map(Project::acceptsEntries).orElse(false);
        if (!projectOpen) {
            return false;
        }
        return line.taskId() == null || tasks.find(new TaskId(line.taskId())).map(Task::acceptsEntries).orElse(false);
    }

    private static Line lineOf(Entry e) {
        return new Line(e.getProjectId().getId(), e.getTaskId() == null ? null : e.getTaskId().getId());
    }
}
