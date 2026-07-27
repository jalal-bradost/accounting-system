package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.platform;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;

import java.util.Optional;
import java.util.UUID;

public interface PlatformUserLookupPort {

    record UserInfo(UUID id, String displayName, String email) {}

    boolean userExistsInCompany(CompanyId companyId, UserId userId);

    Optional<UserInfo> findUser(CompanyId companyId, UserId userId);

    boolean isUserLinkedToAnotherEmployee(CompanyId companyId, UserId userId, UUID excludeEmployeeId);
}
