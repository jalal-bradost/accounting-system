package com.bradox.delin.integration;

import com.bradox.delin.dataaccess.repository.CompanyCurrencyJpaRepository;
import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.hr.service.domain.ports.output.accounting.CompanyCurrencyPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CompanyCurrencyAdapter implements CompanyCurrencyPort {

    private final CompanyCurrencyJpaRepository companyCurrencyJpaRepository;

    public CompanyCurrencyAdapter(CompanyCurrencyJpaRepository companyCurrencyJpaRepository) {
        this.companyCurrencyJpaRepository = companyCurrencyJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveCurrency(CompanyId companyId, String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return false;
        }
        return companyCurrencyJpaRepository
                .findByCompanyIdAndCodeIgnoreCase(companyId.getId(), currencyCode.trim())
                .filter(c -> c.isActive())
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public String defaultCurrencyCode(CompanyId companyId) {
        return companyCurrencyJpaRepository.findByCompanyIdOrderByBaseCurrencyDescCodeAsc(companyId.getId()).stream()
                .filter(c -> c.isBaseCurrency() && c.isActive())
                .map(c -> c.getCode())
                .findFirst()
                .orElseGet(() -> companyCurrencyJpaRepository
                        .findByCompanyIdOrderByBaseCurrencyDescCodeAsc(companyId.getId()).stream()
                        .filter(c -> c.isActive())
                        .map(c -> c.getCode())
                        .findFirst()
                        .orElse("USD"));
    }
}
