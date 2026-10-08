package com.bradox.erp.timesheet.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Reads HR employees without importing HR. Implemented in {@code infrastructure}. */
public interface EmployeeLookupPort {

    record EmployeeRef(UUID id, String name, UUID managerId, UUID departmentId) {
    }

    /** The employee linked to a platform user (D12), if any. */
    Optional<EmployeeRef> findByUser(CompanyId companyId, UUID userId);

    Optional<EmployeeRef> find(CompanyId companyId, UUID employeeId);

    Map<UUID, EmployeeRef> findAll(CompanyId companyId, Collection<UUID> ids);

    /** The manager of a department, for the approver chain (D4). */
    Optional<UUID> departmentManager(CompanyId companyId, UUID departmentId);

    Map<UUID, String> departmentNames(CompanyId companyId, Collection<UUID> departmentIds);

    /** Employees whose HR manager, or whose department manager, is the given employee. */
    List<EmployeeRef> findTeam(CompanyId companyId, UUID managerEmployeeId);

    /** Platform user ids of the employees that are linked to a user (D12). Unlinked employees are absent. */
    Map<UUID, UUID> userIds(CompanyId companyId, Collection<UUID> employeeIds);

    /** Active employees, for pickers. */
    List<EmployeeRef> listActive(CompanyId companyId);
}
