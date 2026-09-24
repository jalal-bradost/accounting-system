package com.bradox.delin.accounting.service.domain.ports.output.repository;

import com.bradox.delin.domain.core.entity.Account;
import com.bradox.delin.domain.core.ValueObject.AccountId;
import com.bradox.delin.domain.valueobject.CompanyId;

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
