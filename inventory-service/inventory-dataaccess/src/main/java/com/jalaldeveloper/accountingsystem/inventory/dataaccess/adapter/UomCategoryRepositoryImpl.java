package com.jalaldeveloper.accountingsystem.inventory.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.UomCategoryEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper.UomDataAccessMapper;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.UomCategoryJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UomCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.UomCategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UomCategoryRepositoryImpl implements UomCategoryRepository {

    private final UomCategoryJpaRepository jpa;
    private final UomDataAccessMapper mapper;

    public UomCategoryRepositoryImpl(UomCategoryJpaRepository jpa, UomDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public UomCategory save(UomCategory category) {
        UomCategoryEntity existing = jpa.findById(category.getId().getId()).orElse(null);
        UomCategoryEntity toSave = mapper.categoryDomainToEntity(category, existing);
        return mapper.categoryEntityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<UomCategory> findById(UomCategoryId id) {
        return jpa.findById(id.getId())
                .filter(UomCategoryEntity::isActive)
                .map(mapper::categoryEntityToDomain);
    }

    @Override
    public Optional<UomCategory> findByIdIncludingArchived(UomCategoryId id) {
        return jpa.findById(id.getId()).map(mapper::categoryEntityToDomain);
    }

    @Override
    public List<UomCategory> findByCompany(CompanyId companyId, boolean includeArchived) {
        return jpa.findByCompany(companyId.getId(), includeArchived).stream()
                .map(mapper::categoryEntityToDomain)
                .toList();
    }
}
