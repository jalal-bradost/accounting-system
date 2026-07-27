package com.jalaldeveloper.accountingsystem.hr.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.EmployeeRepository;
import com.jalaldeveloper.accountingsystem.platform.security.AuthorizationPort;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class HrAccessPolicy {

    private final AuthorizationPort authorizationPort;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final EmployeeRepository employeeRepository;

    public HrAccessPolicy(AuthorizationPort authorizationPort,
                          ObjectProvider<CompanyContext> companyContextProvider,
                          EmployeeRepository employeeRepository) {
        this.authorizationPort = authorizationPort;
        this.companyContextProvider = companyContextProvider;
        this.employeeRepository = employeeRepository;
    }

    public boolean canAccessEmployeeSelf(String selfPermission) {
        UserId userId = currentUserId();
        if (userId == null) {
            return true;
        }
        return authorizationPort.hasAny(userId, Set.of(selfPermission));
    }

    public boolean canAccessEmployee(UUID employeeId, String readPermission, String selfPermission) {
        UserId userId = currentUserId();
        if (userId == null) {
            return true;
        }
        if (authorizationPort.hasAny(userId, Set.of(readPermission))) {
            return true;
        }
        if (!authorizationPort.hasAny(userId, Set.of(selfPermission))) {
            return false;
        }
        EmployeeContextResolver resolver = new EmployeeContextResolver(
                companyContextProvider.getIfAvailable(), employeeRepository);
        return resolver.currentEmployeeId().map(id -> id.equals(employeeId)).orElse(false);
    }

    public void requireEmployeeAccess(UUID employeeId, String readPermission, String selfPermission) {
        if (!canAccessEmployee(employeeId, readPermission, selfPermission)) {
            throw new HrDomainException("Access denied for employee: " + employeeId);
        }
    }

    private UserId currentUserId() {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        return ctx != null ? ctx.currentUser().orElse(null) : null;
    }
}
