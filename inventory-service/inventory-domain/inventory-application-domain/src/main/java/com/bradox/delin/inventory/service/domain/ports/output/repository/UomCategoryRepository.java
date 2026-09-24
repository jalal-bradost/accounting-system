package com.bradox.delin.inventory.service.domain.ports.output.repository;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.inventory.domain.core.entity.UomCategory;
import com.bradox.delin.inventory.domain.core.valueobject.UomCategoryId;

import java.util.List;
import java.util.Optional;

public interface UomCategoryRepository {
    UomCategory save(UomCategory category);
    Optional<UomCategory> findById(UomCategoryId id);
    Optional<UomCategory> findByIdIncludingArchived(UomCategoryId id);
    List<UomCategory> findByCompany(CompanyId companyId, boolean includeArchived);
}
