package com.bradox.erp.inventory.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.entity.UnitOfMeasure;
import com.bradox.erp.inventory.domain.core.valueobject.UomCategoryId;
import com.bradox.erp.inventory.domain.core.valueobject.UomId;

import java.util.List;
import java.util.Optional;

public interface UomRepository {
    UnitOfMeasure save(UnitOfMeasure uom);
    Optional<UnitOfMeasure> findById(UomId id);
    Optional<UnitOfMeasure> findByIdIncludingArchived(UomId id);
    List<UnitOfMeasure> findByCategory(UomCategoryId categoryId, boolean includeArchived);
    List<UnitOfMeasure> findByCompany(CompanyId companyId, boolean includeArchived);
}
