package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UnitOfMeasure;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;

import java.util.List;
import java.util.Optional;

public interface UomRepository {
    UnitOfMeasure save(UnitOfMeasure uom);
    Optional<UnitOfMeasure> findById(UomId id);
    Optional<UnitOfMeasure> findByIdIncludingArchived(UomId id);
    List<UnitOfMeasure> findByCategory(UomCategoryId categoryId, boolean includeArchived);
    List<UnitOfMeasure> findByCompany(CompanyId companyId, boolean includeArchived);
}
