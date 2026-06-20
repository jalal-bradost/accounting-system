package com.jalaldeveloper.accountingsystem.hr.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Department;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.DepartmentId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.mapper.HrDataMapper;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.DepartmentApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.DepartmentRepository;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.EmployeeRepository;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;

@Service
@Validated
class DepartmentApplicationServiceImpl implements DepartmentApplicationService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final HrDataMapper mapper;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    DepartmentApplicationServiceImpl(DepartmentRepository departmentRepository,
                                     EmployeeRepository employeeRepository,
                                     HrDataMapper mapper,
                                     ObjectProvider<CompanyContext> companyContextProvider) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.mapper = mapper;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional
    public DepartmentResponse create(CreateDepartmentCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.getCompanyId());
        if (departmentRepository.existsByCompanyIdAndName(companyId, cmd.getName())) {
            throw new IllegalArgumentException("Department already exists with name: " + cmd.getName());
        }
        UUID id = UUID.randomUUID();
        Department department = mapper.createCommandToDepartment(cmd, id, companyId);
        department.validate();
        return toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    public DepartmentResponse update(UUID id, UpdateDepartmentCommand cmd) {
        Department existing = loadOrThrow(id);
        Department updated = Department.builder()
                .id(existing.getId())
                .companyId(existing.getCompanyId())
                .name(cmd.getName() != null ? cmd.getName() : existing.getName())
                .parentId(resolveParentId(cmd, existing))
                .managerId(resolveManagerId(cmd, existing))
                .colorIndex(cmd.getColorIndex() != null ? cmd.getColorIndex() : existing.getColorIndex())
                .build();
        if (existing.isActive() != updated.isActive()) {
            throw new HrDomainException("Use archive/unarchive to change active flag");
        }
        updated.validate();
        return toResponse(departmentRepository.save(updated));
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse get(UUID id) {
        return toResponse(loadOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentSummaryResponse> list(CompanyId companyId, boolean includeArchived) {
        List<Department> departments = departmentRepository.findByCompanyId(companyId, includeArchived);
        Set<UUID> managerIds = new HashSet<>();
        for (Department d : departments) {
            if (d.getManagerId() != null) managerIds.add(d.getManagerId().getId());
        }
        Map<UUID, EmployeeDisplayMeta> managerMeta = employeeRepository.findDisplayMetaByEmployeeIds(managerIds);
        return departments.stream()
                .map(d -> {
                    EmployeeDisplayMeta mgr = d.getManagerId() != null
                            ? managerMeta.get(d.getManagerId().getId()) : null;
                    long count = employeeRepository.countByDepartmentId(d.getId().getId());
                    return mapper.departmentToSummary(
                            d,
                            mgr != null ? mgr.displayName() : null,
                            mgr != null ? mgr.imageUrl() : null,
                            count);
                })
                .toList();
    }

    @Override
    @Transactional
    public DepartmentResponse archive(UUID id) {
        Department department = loadOrThrow(id);
        department.archive(currentUserDisplay());
        return toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    public DepartmentResponse unarchive(UUID id) {
        Department department = loadIncludingArchivedOrThrow(id);
        department.unarchive();
        return toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional(readOnly = true)
    public long countEmployees(UUID departmentId) {
        return employeeRepository.countByDepartmentId(departmentId);
    }

    private DepartmentResponse toResponse(Department department) {
        String managerName = null;
        String managerImageUrl = null;
        if (department.getManagerId() != null) {
            Map<UUID, EmployeeDisplayMeta> meta = employeeRepository.findDisplayMetaByEmployeeIds(
                    List.of(department.getManagerId().getId()));
            EmployeeDisplayMeta mgr = meta.get(department.getManagerId().getId());
            if (mgr != null) {
                managerName = mgr.displayName();
                managerImageUrl = mgr.imageUrl();
            }
        }
        return mapper.departmentToResponse(department, managerName, managerImageUrl);
    }

    private DepartmentId resolveParentId(UpdateDepartmentCommand cmd, Department existing) {
        if (Boolean.TRUE.equals(cmd.getParentIdReset())) return null;
        if (cmd.getParentId() != null) return new DepartmentId(cmd.getParentId());
        return existing.getParentId();
    }

    private EmployeeId resolveManagerId(UpdateDepartmentCommand cmd, Department existing) {
        if (Boolean.TRUE.equals(cmd.getManagerIdReset())) return null;
        if (cmd.getManagerId() != null) return new EmployeeId(cmd.getManagerId());
        return existing.getManagerId();
    }

    private Department loadOrThrow(UUID id) {
        return departmentRepository.findById(new DepartmentId(id))
                .filter(Department::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
    }

    private Department loadIncludingArchivedOrThrow(UUID id) {
        return departmentRepository.findById(new DepartmentId(id))
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
    }

    private CompanyId resolveCompany(UUID explicit) {
        if (explicit != null) return new CompanyId(explicit);
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) {
            return ctx.currentCompany().orElseThrow(() ->
                    new IllegalArgumentException("companyId required"));
        }
        throw new IllegalArgumentException("companyId required");
    }

    private String currentUserDisplay() {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        return ctx == null ? "system" : ctx.currentUserDisplay();
    }
}
