package com.jalaldeveloper.accountingsystem.hr.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.EmployeeEntity;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.mapper.EmployeeDataAccessMapper;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.EmployeeJpaRepository;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Employee;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.EmployeeDisplayMeta;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.EmployeeImageMeta;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class EmployeeRepositoryImpl implements EmployeeRepository {

    private final EmployeeJpaRepository jpaRepository;
    private final EmployeeDataAccessMapper mapper;

    public EmployeeRepositoryImpl(EmployeeJpaRepository jpaRepository, EmployeeDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Employee save(Employee employee) {
        EmployeeEntity existing = jpaRepository.findById(employee.getId().getId()).orElse(null);
        EmployeeEntity toSave = mapper.domainToEntity(employee, existing);
        return mapper.entityToDomain(jpaRepository.save(toSave));
    }

    @Override
    public Optional<Employee> findById(EmployeeId id) {
        return jpaRepository.findById(id.getId())
                .filter(EmployeeEntity::isActive)
                .map(mapper::entityToDomain);
    }

    @Override
    public Optional<Employee> findByIdIncludingArchived(EmployeeId id) {
        return jpaRepository.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public Page<Employee> search(CompanyId companyId, String query, UUID departmentId,
                                 boolean includeArchived, Pageable pageable) {
        Page<EmployeeEntity> page = jpaRepository.search(
                companyId.getId(), query, departmentId, includeArchived, pageable);
        return page.map(mapper::entityToDomain);
    }

    @Override
    public Optional<EmployeeImageMeta> findImageMeta(UUID employeeId) {
        return jpaRepository.findById(employeeId)
                .filter(e -> e.getImageUrl() != null && !e.getImageUrl().isBlank())
                .map(e -> new EmployeeImageMeta(e.getImageUrl(), e.getImageContentType()));
    }

    @Override
    public Map<UUID, EmployeeImageMeta> findImageMetaByEmployeeIds(Collection<UUID> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) return Map.of();
        Map<UUID, EmployeeImageMeta> out = new HashMap<>();
        for (EmployeeEntity e : jpaRepository.findAllById(employeeIds)) {
            if (e.getImageUrl() != null && !e.getImageUrl().isBlank()) {
                out.put(e.getId(), new EmployeeImageMeta(e.getImageUrl(), e.getImageContentType()));
            }
        }
        return out;
    }

    @Override
    public Map<UUID, EmployeeDisplayMeta> findDisplayMetaByEmployeeIds(Collection<UUID> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) return Map.of();
        Map<UUID, EmployeeDisplayMeta> out = new HashMap<>();
        for (EmployeeEntity e : jpaRepository.findAllById(employeeIds)) {
            String imageUrl = (e.getImageUrl() != null && !e.getImageUrl().isBlank()) ? e.getImageUrl() : null;
            out.put(e.getId(), new EmployeeDisplayMeta(e.getDisplayName(), imageUrl));
        }
        return out;
    }

    @Override
    public void updateImage(UUID employeeId, String imageUrl, String contentType) {
        EmployeeEntity e = jpaRepository.findById(employeeId)
                .orElseThrow(() -> new HrDomainException("Employee not found: " + employeeId));
        e.setImageUrl(imageUrl);
        e.setImageContentType(contentType);
        jpaRepository.save(e);
    }

    @Override
    public void clearImage(UUID employeeId) {
        EmployeeEntity e = jpaRepository.findById(employeeId)
                .orElseThrow(() -> new HrDomainException("Employee not found: " + employeeId));
        e.setImageUrl(null);
        e.setImageContentType(null);
        jpaRepository.save(e);
    }

    @Override
    public long countByDepartmentId(UUID departmentId) {
        return jpaRepository.countByDepartmentIdAndActiveTrue(departmentId);
    }
}
