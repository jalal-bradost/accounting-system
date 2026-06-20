package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Department;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.DepartmentId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository {

    Department save(Department department);

    Optional<Department> findById(DepartmentId id);

    List<Department> findByCompanyId(CompanyId companyId, boolean includeArchived);

    Map<UUID, String> findNamesByIds(Collection<UUID> departmentIds);

    boolean existsByCompanyIdAndName(CompanyId companyId, String name);
}
