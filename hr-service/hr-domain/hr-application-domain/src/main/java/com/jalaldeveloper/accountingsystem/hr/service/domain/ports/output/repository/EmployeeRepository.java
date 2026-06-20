package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Employee;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.EmployeeDisplayMeta;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.EmployeeImageMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository {

    Employee save(Employee employee);

    Optional<Employee> findById(EmployeeId id);

    Optional<Employee> findByIdIncludingArchived(EmployeeId id);

    Page<Employee> search(CompanyId companyId, String query, UUID departmentId,
                          boolean includeArchived, Pageable pageable);

    Optional<EmployeeImageMeta> findImageMeta(UUID employeeId);

    Map<UUID, EmployeeImageMeta> findImageMetaByEmployeeIds(Collection<UUID> employeeIds);

    Map<UUID, EmployeeDisplayMeta> findDisplayMetaByEmployeeIds(Collection<UUID> employeeIds);

    void updateImage(UUID employeeId, String imageUrl, String contentType);

    void clearImage(UUID employeeId);

    long countByDepartmentId(UUID departmentId);
}
