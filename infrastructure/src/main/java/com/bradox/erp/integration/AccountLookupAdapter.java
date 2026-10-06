package com.bradox.erp.integration;

import com.bradox.erp.dataaccess.repository.AccountJpaRepository;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.service.domain.ports.output.accounting.AccountLookupPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AccountLookupAdapter implements AccountLookupPort {

    private final AccountJpaRepository accountRepository;

    public AccountLookupAdapter(AccountJpaRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Optional<UUID> findAccountId(CompanyId companyId, String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return accountRepository.findByCompanyIdAndCode(companyId.getId(), code.trim())
                .map(a -> a.getId());
    }
}
