package com.jalaldeveloper.accountingsystem.purchase.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurFiscalTaxEntity;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.mapper.FiscalTaxDataAccessMapper;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.repository.PurFiscalTaxJpaRepository;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.FiscalTax;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository.FiscalTaxRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FiscalTaxRepositoryImpl implements FiscalTaxRepository {

    private final PurFiscalTaxJpaRepository jpa;
    private final FiscalTaxDataAccessMapper mapper;

    public FiscalTaxRepositoryImpl(PurFiscalTaxJpaRepository jpa, FiscalTaxDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public FiscalTax save(FiscalTax tax) {
        PurFiscalTaxEntity existing = jpa.findById(tax.getId()).orElse(null);
        PurFiscalTaxEntity toSave = mapper.domainToEntity(tax, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<FiscalTax> findById(UUID id) {
        return jpa.findById(id).map(mapper::entityToDomain);
    }

    @Override
    public List<FiscalTax> findByCompanyIdAndActive(UUID companyId, boolean active) {
        return jpa.findByCompanyIdAndActive(companyId, active).stream().map(mapper::entityToDomain).toList();
    }
}
