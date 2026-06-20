package com.jalaldeveloper.accountingsystem.hr.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Employee;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.DepartmentId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.mapper.HrDataMapper;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.EmployeeApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.DepartmentRepository;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.EmployeeRepository;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.storage.EmployeeImageStoragePort;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@Validated
class EmployeeApplicationServiceImpl implements EmployeeApplicationService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final HrDataMapper mapper;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final EmployeeImageStoragePort imageStorage;

    EmployeeApplicationServiceImpl(EmployeeRepository employeeRepository,
                                   DepartmentRepository departmentRepository,
                                   HrDataMapper mapper,
                                   ObjectProvider<CompanyContext> companyContextProvider,
                                   EmployeeImageStoragePort imageStorage) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.mapper = mapper;
        this.companyContextProvider = companyContextProvider;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.getCompanyId());
        UUID id = UUID.randomUUID();
        Employee employee = mapper.createCommandToEmployee(cmd, id, companyId);
        employee.validate();
        Employee saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse update(UUID id, UpdateEmployeeCommand cmd) {
        Employee existing = loadOrThrow(id);
        Employee updated = Employee.builder()
                .id(existing.getId())
                .companyId(existing.getCompanyId())
                .displayName(cmd.getDisplayName() != null ? cmd.getDisplayName() : existing.getDisplayName())
                .workEmail(cmd.getWorkEmail() != null ? cmd.getWorkEmail() : existing.getWorkEmail())
                .workPhone(cmd.getWorkPhone() != null ? cmd.getWorkPhone() : existing.getWorkPhone())
                .mobilePhone(cmd.getMobilePhone() != null ? cmd.getMobilePhone() : existing.getMobilePhone())
                .jobTitle(cmd.getJobTitle() != null ? cmd.getJobTitle() : existing.getJobTitle())
                .departmentId(resolveDepartmentId(cmd, existing))
                .managerId(resolveManagerId(cmd, existing))
                .hireDate(cmd.getHireDate() != null ? cmd.getHireDate() : existing.getHireDate())
                .workStreet(cmd.getWorkStreet() != null ? cmd.getWorkStreet() : existing.getWorkStreet())
                .workCity(cmd.getWorkCity() != null ? cmd.getWorkCity() : existing.getWorkCity())
                .workState(cmd.getWorkState() != null ? cmd.getWorkState() : existing.getWorkState())
                .workPostalCode(cmd.getWorkPostalCode() != null ? cmd.getWorkPostalCode() : existing.getWorkPostalCode())
                .workCountry(cmd.getWorkCountry() != null ? cmd.getWorkCountry() : existing.getWorkCountry())
                .workLocation(cmd.getWorkLocation() != null ? cmd.getWorkLocation() : existing.getWorkLocation())
                .build();
        if (existing.isActive() != updated.isActive()) {
            throw new HrDomainException("Use archive/unarchive to change active flag");
        }
        updated.validate();
        Employee saved = employeeRepository.save(updated);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse get(UUID id) {
        return toResponse(loadOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeSummaryResponse> search(CompanyId companyId, String q, UUID departmentId,
                                                boolean includeArchived, Pageable pageable) {
        Page<Employee> page = employeeRepository.search(companyId, q, departmentId, includeArchived, pageable);
        Set<UUID> departmentIds = new HashSet<>();
        Set<UUID> managerIds = new HashSet<>();
        for (Employee e : page.getContent()) {
            if (e.getDepartmentId() != null) departmentIds.add(e.getDepartmentId().getId());
            if (e.getManagerId() != null) managerIds.add(e.getManagerId().getId());
        }
        Map<UUID, String> departmentNames = departmentRepository.findNamesByIds(departmentIds);
        Map<UUID, EmployeeDisplayMeta> managerMeta = employeeRepository.findDisplayMetaByEmployeeIds(managerIds);
        Map<UUID, EmployeeImageMeta> imageMeta = employeeRepository.findImageMetaByEmployeeIds(
                page.getContent().stream().map(e -> e.getId().getId()).toList());

        return page.map(e -> {
            String deptName = e.getDepartmentId() != null
                    ? departmentNames.get(e.getDepartmentId().getId()) : null;
            EmployeeDisplayMeta mgr = e.getManagerId() != null
                    ? managerMeta.get(e.getManagerId().getId()) : null;
            EmployeeImageMeta img = imageMeta.get(e.getId().getId());
            return mapper.employeeToSummary(
                    e,
                    deptName,
                    mgr != null ? mgr.displayName() : null,
                    mgr != null ? mgr.imageUrl() : null,
                    img != null ? img.imageUrl() : null);
        });
    }

    @Override
    @Transactional
    public EmployeeResponse archive(UUID id) {
        Employee employee = loadOrThrow(id);
        employee.archive(currentUserDisplay());
        return toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public EmployeeResponse unarchive(UUID id) {
        Employee employee = loadIncludingArchivedOrThrow(id);
        employee.unarchive();
        return toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public EmployeeResponse uploadImage(UUID id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new HrDomainException("Image file is required");
        }
        Employee employee = loadIncludingArchivedOrThrow(id);
        employeeRepository.findImageMeta(id).ifPresent(meta -> imageStorage.deleteIfPresent(meta.imageUrl()));
        EmployeeImageStoragePort.StoredImage stored;
        try {
            stored = imageStorage.store(
                    employee.getCompanyId().getId(),
                    id,
                    file.getContentType(),
                    file.getSize(),
                    file.getInputStream());
        } catch (IOException ex) {
            throw new HrDomainException("Failed to read uploaded image");
        }
        employeeRepository.updateImage(id, stored.publicUrl(), stored.contentType());
        return get(id);
    }

    @Override
    @Transactional
    public EmployeeResponse deleteImage(UUID id) {
        loadIncludingArchivedOrThrow(id);
        employeeRepository.findImageMeta(id).ifPresent(meta -> imageStorage.deleteIfPresent(meta.imageUrl()));
        employeeRepository.clearImage(id);
        return get(id);
    }

    private EmployeeResponse toResponse(Employee employee) {
        String departmentName = null;
        if (employee.getDepartmentId() != null) {
            departmentName = departmentRepository.findById(employee.getDepartmentId())
                    .map(d -> d.getName())
                    .orElse(null);
        }
        String managerName = null;
        String managerImageUrl = null;
        if (employee.getManagerId() != null) {
            Map<UUID, EmployeeDisplayMeta> meta = employeeRepository.findDisplayMetaByEmployeeIds(
                    List.of(employee.getManagerId().getId()));
            EmployeeDisplayMeta mgr = meta.get(employee.getManagerId().getId());
            if (mgr != null) {
                managerName = mgr.displayName();
                managerImageUrl = mgr.imageUrl();
            }
        }
        EmployeeImageMeta imageMeta = employeeRepository.findImageMeta(employee.getId().getId()).orElse(null);
        return mapper.employeeToResponse(
                employee,
                departmentName,
                managerName,
                managerImageUrl,
                imageMeta != null ? imageMeta.imageUrl() : null,
                imageMeta != null ? imageMeta.contentType() : null);
    }

    private DepartmentId resolveDepartmentId(UpdateEmployeeCommand cmd, Employee existing) {
        if (Boolean.TRUE.equals(cmd.getDepartmentIdReset())) return null;
        if (cmd.getDepartmentId() != null) return new DepartmentId(cmd.getDepartmentId());
        return existing.getDepartmentId();
    }

    private EmployeeId resolveManagerId(UpdateEmployeeCommand cmd, Employee existing) {
        if (Boolean.TRUE.equals(cmd.getManagerIdReset())) return null;
        if (cmd.getManagerId() != null) return new EmployeeId(cmd.getManagerId());
        return existing.getManagerId();
    }

    private Employee loadOrThrow(UUID id) {
        return employeeRepository.findById(new EmployeeId(id))
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
    }

    private Employee loadIncludingArchivedOrThrow(UUID id) {
        return employeeRepository.findByIdIncludingArchived(new EmployeeId(id))
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
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
