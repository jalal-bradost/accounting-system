package com.bradox.delin.inventory.service.domain.ports.output.repository;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.inventory.domain.core.entity.UnitOfMeasure;
import com.bradox.delin.inventory.domain.core.valueobject.UomCategoryId;
import com.bradox.delin.inventory.domain.core.valueobject.UomId;

import java.util.List;
import java.util.Optional;

public interface UomRepository {
    UnitOfMeasure save(UnitOfMeasure uom);
    Optional<UnitOfMeasure> findById(UomId id);
    Optional<UnitOfMeasure> findByIdIncludingArchived(UomId id);
    List<UnitOfMeasure> findByCategory(UomCategoryId categoryId, boolean includeArchived);
    List<UnitOfMeasure> findByCompany(CompanyId companyId, boolean includeArchived);
}
