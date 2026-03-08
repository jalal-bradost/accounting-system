package com.jalaldeveloper.accountingsystem.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccountEntity;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Account;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountDataAccessMapper {

    public Account entityToDomain(AccountEntity entity) {
        if (entity == null) return null;
        return Account.builder()
                .id(new AccountId(entity.getId()))
                .companyId(new CompanyId(entity.getCompanyId()))
                .code(entity.getCode())
                .name(entity.getName())
                .accountType(entity.getType())
                .active(entity.isActive())
                .build();
    }

    public AccountEntity domainToEntity(Account domain) {
        if (domain == null) return null;
        return AccountEntity.builder()
                .id(domain.getId().getId())
                .companyId(domain.getCompanyId().getId())
                .code(domain.getCode())
                .name(domain.getName())
                .type(domain.getAccountType())
                .active(domain.isActive())
                .build();
    }
}
