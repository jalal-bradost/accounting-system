package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccountEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.mapper.AccountDataAccessMapper;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccountJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Account;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountJpaRepository jpaRepository;
    private final AccountDataAccessMapper mapper;

    public AccountRepositoryImpl(AccountJpaRepository jpaRepository, AccountDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = mapper.domainToEntity(account);
        AccountEntity saved = jpaRepository.save(entity);
        return mapper.entityToDomain(saved);
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return jpaRepository.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public Optional<Account> findByCompanyIdAndCode(CompanyId companyId, String code) {
        return jpaRepository.findByCompanyIdAndCode(companyId.getId(), code).map(mapper::entityToDomain);
    }

    @Override
    public List<Account> findByCompanyId(CompanyId companyId) {
        return jpaRepository.findByCompanyId(companyId.getId()).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCompanyIdAndCode(CompanyId companyId, String code) {
        return jpaRepository.existsByCompanyIdAndCode(companyId.getId(), code);
    }
}
