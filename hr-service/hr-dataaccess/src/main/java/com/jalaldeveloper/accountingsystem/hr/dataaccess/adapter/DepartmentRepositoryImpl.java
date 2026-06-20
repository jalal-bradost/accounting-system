package com.jalaldeveloper.accountingsystem.hr.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.DepartmentEntity;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.mapper.DepartmentDataAccessMapper;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.DepartmentJpaRepository;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Department;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.DepartmentId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.DepartmentRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class DepartmentRepositoryImpl implements DepartmentRepository {

    private final DepartmentJpaRepository jpaRepository;
    private final DepartmentDataAccessMapper mapper;

    public DepartmentRepositoryImpl(DepartmentJpaRepository jpaRepository,
                                    DepartmentDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Department save(Department department) {
        DepartmentEntity existing = jpaRepository.findById(department.getId().getId()).orElse(null);
        DepartmentEntity toSave = mapper.domainToEntity(department, existing);
        return mapper.entityToDomain(jpaRepository.save(toSave));
    }

    @Override
    public Optional<Department> findById(DepartmentId id) {
        return jpaRepository.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public List<Department> findByCompanyId(CompanyId companyId, boolean includeArchived) {
        List<DepartmentEntity> entities = includeArchived
                ? jpaRepository.findByCompanyIdOrderByNameAsc(companyId.getId())
                : jpaRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId.getId());
        return entities.stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public Map<UUID, String> findNamesByIds(Collection<UUID> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) return Map.of();
        Map<UUID, String> out = new HashMap<>();
        for (DepartmentEntity e : jpaRepository.findAllById(departmentIds)) {
            out.put(e.getId(), e.getName());
        }
        return out;
    }

    @Override
    public boolean existsByCompanyIdAndName(CompanyId companyId, String name) {
        return jpaRepository.existsByCompanyIdAndName(companyId.getId(), name);
    }
}
