package com.jalaldeveloper.accountingsystem.hr.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.EmployeeJpaRepository;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.platform.PlatformUserLookupPort;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AppUserEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.AppUserJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class PlatformUserLookupAdapter implements PlatformUserLookupPort {

    private final AppUserJpaRepository appUserJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public PlatformUserLookupAdapter(AppUserJpaRepository appUserJpaRepository,
                                       EmployeeJpaRepository employeeJpaRepository) {
        this.appUserJpaRepository = appUserJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExistsInCompany(CompanyId companyId, UserId userId) {
        return appUserJpaRepository.findById(userId.getId())
                .filter(u -> u.getCompanyId().equals(companyId.getId()))
                .filter(AppUserEntity::isActive)
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserInfo> findUser(CompanyId companyId, UserId userId) {
        return appUserJpaRepository.findById(userId.getId())
                .filter(u -> u.getCompanyId().equals(companyId.getId()))
                .map(u -> new UserInfo(u.getId(), u.getDisplayName(), u.getEmail()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserLinkedToAnotherEmployee(CompanyId companyId, UserId userId, UUID excludeEmployeeId) {
        return employeeJpaRepository.findByCompanyIdAndUserId(companyId.getId(), userId.getId())
                .filter(e -> !e.getId().equals(excludeEmployeeId))
                .isPresent();
    }
}
