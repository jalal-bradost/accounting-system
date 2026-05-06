package com.jalaldeveloper.accountingsystem.contacts.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity.PartnerEntity;
import com.jalaldeveloper.accountingsystem.contacts.dataaccess.mapper.PartnerDataAccessMapper;
import com.jalaldeveloper.accountingsystem.contacts.dataaccess.repository.PartnerJpaRepository;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.Partner;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.repository.PartnerRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PartnerRepositoryImpl implements PartnerRepository {

    private final PartnerJpaRepository jpaRepository;
    private final PartnerDataAccessMapper mapper;

    public PartnerRepositoryImpl(PartnerJpaRepository jpaRepository, PartnerDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Partner save(Partner partner) {
        PartnerEntity existing = jpaRepository.findById(partner.getId().getId()).orElse(null);
        PartnerEntity toSave = mapper.domainToEntity(partner, existing);
        return mapper.entityToDomain(jpaRepository.save(toSave));
    }

    @Override
    public Optional<Partner> findById(PartnerId id) {
        return jpaRepository.findById(id.getId())
                .filter(PartnerEntity::isActive)
                .map(mapper::entityToDomain);
    }

    @Override
    public Optional<Partner> findByIdIncludingArchived(PartnerId id) {
        return jpaRepository.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public Page<Partner> search(CompanyId companyId, String query, Boolean isCustomer, Boolean isVendor,
                                boolean includeArchived, Pageable pageable) {
        Page<PartnerEntity> page = jpaRepository.search(
                companyId.getId(), query, isCustomer, isVendor, includeArchived, pageable);
        return page.map(mapper::entityToDomain);
    }
}
