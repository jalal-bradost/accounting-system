package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.core.entity.Account;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for Account persistence (hexagonal architecture).
 */
public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(AccountId id);

    Optional<Account> findByCompanyIdAndCode(CompanyId companyId, String code);

    List<Account> findByCompanyId(CompanyId companyId);

    boolean existsByCompanyIdAndCode(CompanyId companyId, String code);
}
