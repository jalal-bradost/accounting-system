package com.jalaldeveloper.accountingsystem.inventory.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.UomEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper.UomDataAccessMapper;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.UomJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UnitOfMeasure;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.UomRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UomRepositoryImpl implements UomRepository {

    private final UomJpaRepository jpa;
    private final UomDataAccessMapper mapper;

    public UomRepositoryImpl(UomJpaRepository jpa, UomDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public UnitOfMeasure save(UnitOfMeasure uom) {
        UomEntity existing = jpa.findById(uom.getId().getId()).orElse(null);
        UomEntity toSave = mapper.domainToEntity(uom, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<UnitOfMeasure> findById(UomId id) {
        return jpa.findById(id.getId())
                .filter(UomEntity::isActive)
                .map(mapper::entityToDomain);
    }

    @Override
    public Optional<UnitOfMeasure> findByIdIncludingArchived(UomId id) {
        return jpa.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public List<UnitOfMeasure> findByCategory(UomCategoryId categoryId, boolean includeArchived) {
        return jpa.findByCategory(categoryId.getId(), includeArchived).stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public List<UnitOfMeasure> findByCompany(CompanyId companyId, boolean includeArchived) {
        return jpa.findByCompany(companyId.getId(), includeArchived).stream()
                .map(mapper::entityToDomain)
                .toList();
    }
}
