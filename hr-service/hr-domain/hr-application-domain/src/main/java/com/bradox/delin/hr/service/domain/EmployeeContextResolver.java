package com.bradox.delin.hr.service.domain;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.domain.valueobject.UserId;
import com.bradox.delin.hr.domain.core.entity.Employee;
import com.bradox.delin.hr.service.domain.ports.output.repository.EmployeeRepository;
import com.bradox.delin.platform.web.CompanyContext;

import java.util.Optional;
import java.util.UUID;

public class EmployeeContextResolver {

    private final CompanyContext companyContext;
    private final EmployeeRepository employeeRepository;

    public EmployeeContextResolver(CompanyContext companyContext, EmployeeRepository employeeRepository) {
        this.companyContext = companyContext;
        this.employeeRepository = employeeRepository;
    }

    public Optional<UUID> currentEmployeeId() {
        if (companyContext == null) {
            return Optional.empty();
        }
        Optional<UserId> userId = companyContext.currentUser();
        Optional<CompanyId> companyId = companyContext.currentCompany();
        if (userId.isEmpty() || companyId.isEmpty()) {
            return Optional.empty();
        }
        if (employeeRepository == null) {
            return Optional.empty();
        }
        return employeeRepository.findByUserId(companyId.get(), userId.get())
                .map(e -> e.getId().getId());
    }

    public Optional<Employee> currentEmployee() {
        if (companyContext == null || employeeRepository == null) {
            return Optional.empty();
        }
        Optional<UserId> userId = companyContext.currentUser();
        Optional<CompanyId> companyId = companyContext.currentCompany();
        if (userId.isEmpty() || companyId.isEmpty()) {
            return Optional.empty();
        }
        return employeeRepository.findByUserId(companyId.get(), userId.get());
    }
}
