package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.rule.DurationParser;
import com.bradox.erp.timesheet.domain.core.valueobject.EntryId;
import com.bradox.erp.timesheet.service.domain.dto.CopyEntriesCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryListResponse;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.EntryApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository.Filter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Validated
class EntryApplicationServiceImpl implements EntryApplicationService {

    private final EntryRepository entries;
    private final EntryWriter writer;
    private final EntryAssembler assembler;
    private final TimesheetAccess access;

    EntryApplicationServiceImpl(EntryRepository entries, EntryWriter writer, EntryAssembler assembler,
                                TimesheetAccess access) {
        this.entries = entries;
        this.writer = writer;
        this.assembler = assembler;
        this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public EntryListResponse list(CompanyId companyId, UUID employeeId, boolean all, LocalDate from, LocalDate to,
                                  UUID projectId, UUID taskId, Boolean billable) {
        UUID scope;
        if (all && employeeId == null) {
            access.require(TimesheetPermissions.ENTRY_VIEW_ALL);
            scope = null;
        } else if (employeeId == null && access.actor(companyId).isEmpty()) {
            return new EntryListResponse(List.of(), 0, 0);
        } else {
            scope = access.resolveForRead(companyId, employeeId).id();
        }
        List<Entry> found = entries.search(companyId, new Filter(scope, from, to, projectId, taskId, billable));
        long total = found.stream().mapToLong(Entry::getMinutes).sum();
        long billed = found.stream().filter(Entry::isBillable).mapToLong(Entry::getMinutes).sum();
        return new EntryListResponse(assembler.toResponses(companyId, found, null), total, billed);
    }

    @Override
    @Transactional(readOnly = true)
    public EntryListResponse byRecord(CompanyId companyId, String model, UUID recordId) {
        boolean all = access.can(TimesheetPermissions.ENTRY_VIEW_ALL);
        UUID me = access.actor(companyId).map(a -> a.id()).orElse(null);
        List<Entry> found = entries.findByRecord(companyId, model, recordId).stream()
                .filter(e -> all || e.getEmployeeId().equals(me)).toList();
        long total = found.stream().mapToLong(Entry::getMinutes).sum();
        long billed = found.stream().filter(Entry::isBillable).mapToLong(Entry::getMinutes).sum();
        return new EntryListResponse(assembler.toResponses(companyId, found, null), total, billed);
    }

    @Override
    @Transactional(readOnly = true)
    public EntryResponse get(CompanyId companyId, UUID id) {
        Entry e = load(companyId, id);
        boolean mine = access.actor(companyId).map(a -> a.id().equals(e.getEmployeeId())).orElse(false);
        if (!mine) {
            access.require(TimesheetPermissions.ENTRY_VIEW_ALL);
        }
        return assembler.toResponse(companyId, e, null);
    }

    @Override
    @Transactional
    public EntryResponse log(CompanyId companyId, EntryCommand c) {
        var target = access.resolveForWrite(companyId, c.employeeId());
        EntryWriter.Saved saved = writer.create(companyId, target, c.workDate(), minutesOf(c, 0), c.projectId(),
                c.taskId(), c.description(), c.billable());
        return assembler.toResponse(companyId, saved.entry(), saved.warning());
    }

    @Override
    @Transactional
    public EntryResponse update(CompanyId companyId, UUID id, EntryCommand c) {
        Entry existing = load(companyId, id);
        access.requireModifiable(companyId, existing.getEmployeeId());
        boolean keepTarget = c.projectId() == null && c.taskId() == null;
        EntryWriter.Saved saved = writer.update(companyId, existing,
                c.workDate() == null ? existing.getWorkDate() : c.workDate(),
                minutesOf(c, existing.getMinutes()),
                keepTarget ? existing.getProjectId().getId() : c.projectId(),
                keepTarget ? (existing.getTaskId() == null ? null : existing.getTaskId().getId()) : c.taskId(),
                c.description(), c.billable());
        return assembler.toResponse(companyId, saved.entry(), saved.warning());
    }

    @Override
    @Transactional
    public void delete(CompanyId companyId, UUID id) {
        Entry existing = load(companyId, id);
        access.requireModifiable(companyId, existing.getEmployeeId());
        writer.delete(companyId, existing);
    }

    @Override
    @Transactional
    public List<EntryResponse> copy(CompanyId companyId, CopyEntriesCommand c) {
        var target = access.resolveForWrite(companyId, c.employeeId());
        if (c.fromDate().equals(c.toDate())) {
            throw new TimesheetDomainException("error.timesheet.copySameDay", null,
                    "Pick a different day to copy to");
        }
        List<Entry> source = entries.findForEmployee(companyId, target.employee().id(), c.fromDate(), c.fromDate());
        List<Entry> created = new ArrayList<>();
        Map<UUID, String> warnings = new HashMap<>();
        for (Entry e : source) {
            try {
                EntryWriter.Saved saved = writer.create(companyId, target, c.toDate(), e.getMinutes(),
                        e.getProjectId().getId(), e.getTaskId() == null ? null : e.getTaskId().getId(),
                        e.getDescription(), e.isBillable());
                created.add(saved.entry());
                if (saved.warning() != null) {
                    warnings.put(saved.entry().getId().getId(), saved.warning());
                }
            } catch (TimesheetDomainException ex) {
                // A project or task closed since yesterday is skipped; every other rule still stops the copy.
                String key = ex.getMessageKey();
                if (!"error.timesheet.projectClosed".equals(key) && !"error.timesheet.taskClosed".equals(key)) {
                    throw ex;
                }
            }
        }
        return assembler.toResponses(companyId, created, warnings);
    }

    private Entry load(CompanyId companyId, UUID id) {
        return entries.find(new EntryId(id)).filter(e -> e.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time entry not found"));
    }

    /** TSH-02 #2: free-text duration wins over a numeric one. */
    private static int minutesOf(EntryCommand c, int fallback) {
        if (c.duration() != null && !c.duration().isBlank()) {
            return DurationParser.parse(c.duration());
        }
        return c.minutes() != null ? c.minutes() : fallback;
    }
}
