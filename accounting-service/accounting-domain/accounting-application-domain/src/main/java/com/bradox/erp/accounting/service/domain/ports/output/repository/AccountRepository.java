package com.bradox.erp.accounting.service.domain.ports.output.repository;

import com.bradox.erp.domain.core.entity.Account;
import com.bradox.erp.domain.core.ValueObject.AccountId;
import com.bradox.erp.domain.valueobject.CompanyId;

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
