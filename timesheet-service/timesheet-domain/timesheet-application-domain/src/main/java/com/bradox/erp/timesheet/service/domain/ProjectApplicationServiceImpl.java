package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.service.domain.dto.ProjectCommand;
import com.bradox.erp.timesheet.service.domain.dto.ProjectResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.ProjectApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.PartnerLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
class ProjectApplicationServiceImpl implements ProjectApplicationService {

    static final String AUDIT_MODEL = "tsh.project";

    private final ProjectRepository projects;
    private final EntryRepository entries;
    private final EmployeeLookupPort employees;
    private final PartnerLookupPort partners;
    private final AuditLogPort audit;
    private final TimesheetAccess access;
    private final SalesLineLookupPort salesLines;

    ProjectApplicationServiceImpl(ProjectRepository projects, EntryRepository entries, EmployeeLookupPort employees,
                                  PartnerLookupPort partners, AuditLogPort audit, TimesheetAccess access,
                                  SalesLineLookupPort salesLines) {
        this.projects = projects;
        this.entries = entries;
        this.employees = employees;
        this.partners = partners;
        this.audit = audit;
        this.access = access;
        this.salesLines = salesLines;
    }

    @Override
    @Transactional
    public List<ProjectResponse> list(CompanyId companyId, boolean includeArchived) {
        ensureInternal(companyId);
        List<Project> all = projects.findAll(companyId, includeArchived).stream()
                .sorted(Comparator.comparing((Project p) -> p.getName().toLowerCase())).toList();
        return toResponses(companyId, all);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse get(CompanyId companyId, UUID id) {
        return toResponses(companyId, List.of(load(companyId, id))).get(0);
    }

    @Override
    @Transactional
    public ProjectResponse create(CompanyId companyId, ProjectCommand command) {
        access.require(TimesheetPermissions.PROJECT_MANAGE);
        checkReferences(companyId, command);
        checkCode(companyId, command.code(), null);
        Project p = Project.create(new ProjectId(UUID.randomUUID()), companyId, command.name(), command.code(),
                command.partnerId(), command.managerEmployeeId(), !Boolean.FALSE.equals(command.allowTimesheets()),
                command.billingMode(), Boolean.TRUE.equals(command.billableDefault()), command.allocatedMinutes(),
                command.color(), null, access.clock().instant(), access.actorLabel());
        applyAccounting(companyId, p, command);
        Project saved = projects.save(p);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, saved.getId().getId(), "Project created", summary(saved));
        return toResponses(companyId, List.of(saved)).get(0);
    }

    @Override
    @Transactional
    public ProjectResponse update(CompanyId companyId, UUID id, ProjectCommand command) {
        access.require(TimesheetPermissions.PROJECT_MANAGE);
        Project p = load(companyId, id);
        checkReferences(companyId, command);
        checkCode(companyId, command.code(), p.getId());
        p.update(command.name(), command.code(), command.partnerId(), command.managerEmployeeId(),
                !Boolean.FALSE.equals(command.allowTimesheets()),
                command.billingMode() == null ? p.getBillingMode() : command.billingMode(),
                Boolean.TRUE.equals(command.billableDefault()), command.allocatedMinutes(), command.color());
        applyAccounting(companyId, p, command);
        Project saved = projects.save(p);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, saved.getId().getId(), "Project updated", summary(saved));
        return toResponses(companyId, List.of(saved)).get(0);
    }

    @Override
    @Transactional
    public ProjectResponse archive(CompanyId companyId, UUID id) {
        access.require(TimesheetPermissions.PROJECT_MANAGE);
        Project p = load(companyId, id);
        p.archive();
        Project saved = projects.save(p);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Project archived", Map.of());
        return toResponses(companyId, List.of(saved)).get(0);
    }

    @Override
    @Transactional
    public ProjectResponse unarchive(CompanyId companyId, UUID id) {
        access.require(TimesheetPermissions.PROJECT_MANAGE);
        Project p = load(companyId, id);
        p.unarchive();
        Project saved = projects.save(p);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Project restored", Map.of());
        return toResponses(companyId, List.of(saved)).get(0);
    }

    /** The default order line needs billing rights and the cost account needs posting rights (permission table). */
    private void applyAccounting(CompanyId companyId, Project p, ProjectCommand c) {
        UUID line = p.getBillingMode() == BillingMode.FIXED_PRICE ? null : c.defaultSaleLineId();
        if (!java.util.Objects.equals(line, p.getDefaultSaleLineId())) {
            access.require(TimesheetPermissions.BILLING_MANAGE);
            if (line != null) {
                var info = salesLines.find(companyId, line).orElseThrow(() -> new TimesheetDomainException(
                        "error.timesheet.saleLineNotFound", null, "Sales order line not found"));
                if (!info.eligible()) {
                    throw new TimesheetDomainException("error.timesheet.saleLineNotEligible", null,
                            "That line is not a service line invoiced from timesheets");
                }
            }
        }
        if (!java.util.Objects.equals(c.costAccountId(), p.getCostAccountId())) {
            access.require(TimesheetPermissions.POSTING_MANAGE);
        }
        p.configureAccounting(line, c.costAccountId());
    }

    /** TSH-01 #9: a non-billable "Internal" project exists from the start. Idempotent. */
    private void ensureInternal(CompanyId companyId) {
        if (projects.findBySystemKey(companyId, Project.INTERNAL_KEY).isPresent()) {
            return;
        }
        projects.createIfAbsent(Project.create(new ProjectId(UUID.randomUUID()), companyId, "Internal", null, null, null, true,
                BillingMode.HOURLY, false, null, "#0ea5e9", Project.INTERNAL_KEY, access.clock().instant(), "system"));
    }

    private Project load(CompanyId companyId, UUID id) {
        return projects.find(new ProjectId(id)).filter(p -> p.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    /** TSH-01 #6. */
    private void checkCode(CompanyId companyId, String code, ProjectId self) {
        String normalized = Project.normalizeCode(code);
        if (normalized != null && projects.codeTaken(companyId, normalized, self)) {
            throw new TimesheetDomainException("error.timesheet.projectCodeTaken", new Object[]{code},
                    "A project with code " + code + " already exists");
        }
    }

    private void checkReferences(CompanyId companyId, ProjectCommand command) {
        if (command.partnerId() != null && !partners.exists(companyId, command.partnerId())) {
            throw new TimesheetDomainException("error.timesheet.customerNotFound", null, "Customer not found");
        }
        if (command.managerEmployeeId() != null && employees.find(companyId, command.managerEmployeeId()).isEmpty()) {
            throw new TimesheetDomainException("error.timesheet.employeeNotFound", null, "Manager not found");
        }
    }

    private List<ProjectResponse> toResponses(CompanyId companyId, List<Project> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> logged = entries.minutesByProject(companyId,
                list.stream().map(p -> p.getId().getId()).toList());
        Map<UUID, String> partnerNames = partners.names(companyId,
                list.stream().map(Project::getPartnerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<UUID, EmployeeRef> managers = employees.findAll(companyId,
                list.stream().map(Project::getManagerEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return list.stream().map(p -> new ProjectResponse(p.getId().getId(), p.getName(), p.getCode(),
                p.getPartnerId(), p.getPartnerId() == null ? null : partnerNames.get(p.getPartnerId()),
                p.getManagerEmployeeId(),
                p.getManagerEmployeeId() == null || !managers.containsKey(p.getManagerEmployeeId()) ? null
                        : managers.get(p.getManagerEmployeeId()).name(),
                p.isAllowTimesheets(), p.getBillingMode(), p.isBillableDefault(), p.getAllocatedMinutes(),
                logged.getOrDefault(p.getId().getId(), 0L), p.getStatus(), p.getColor(), p.isSystemManaged(),
                Project.INTERNAL_KEY.equals(p.getSystemKey()), p.getDefaultSaleLineId(),
                p.getDefaultSaleLineId() == null ? null : salesLines.find(companyId, p.getDefaultSaleLineId())
                        .map(SalesLineLookupPort.SaleLine::label).orElse(null),
                p.getCostAccountId())).toList();
    }

    private static Map<String, Object> summary(Project p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", p.getName());
        m.put("code", p.getCode());
        m.put("billingMode", p.getBillingMode().name());
        m.put("allowTimesheets", p.isAllowTimesheets());
        return m;
    }
}
