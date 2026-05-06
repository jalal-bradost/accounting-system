package com.jalaldeveloper.accountingsystem.contacts.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity.PaymentTermsEntity;
import com.jalaldeveloper.accountingsystem.contacts.dataaccess.mapper.PaymentTermsDataAccessMapper;
import com.jalaldeveloper.accountingsystem.contacts.dataaccess.repository.PaymentTermsJpaRepository;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PaymentTerms;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PaymentTermsId;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.repository.PaymentTermsRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PaymentTermsRepositoryImpl implements PaymentTermsRepository {

    private final PaymentTermsJpaRepository jpaRepository;
    private final PaymentTermsDataAccessMapper mapper;

    public PaymentTermsRepositoryImpl(PaymentTermsJpaRepository jpaRepository,
                                      PaymentTermsDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PaymentTerms save(PaymentTerms terms) {
        PaymentTermsEntity existing = jpaRepository.findById(terms.getId().getId()).orElse(null);
        PaymentTermsEntity toSave = mapper.domainToEntity(terms, existing);
        return mapper.entityToDomain(jpaRepository.save(toSave));
    }

    @Override
    public Optional<PaymentTerms> findById(PaymentTermsId id) {
        return jpaRepository.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public List<PaymentTerms> findByCompanyId(CompanyId companyId, boolean includeArchived) {
        List<PaymentTermsEntity> entities = includeArchived
                ? jpaRepository.findByCompanyIdOrderByNameAsc(companyId.getId())
                : jpaRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId.getId());
        return entities.stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public boolean existsByCompanyIdAndName(CompanyId companyId, String name) {
        return jpaRepository.existsByCompanyIdAndName(companyId.getId(), name);
    }
}
