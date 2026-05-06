package com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.UomCategoryEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.UomEntity;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UnitOfMeasure;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UomCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;
import org.springframework.stereotype.Component;

@Component
public class UomDataAccessMapper {

    public UomCategory categoryEntityToDomain(UomCategoryEntity e) {
        if (e == null) return null;
        UomCategory.Builder b = UomCategory.builder()
                .id(new UomCategoryId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .name(e.getName());
        if (!e.isActive()) {
            b.archived(true).archivedAt(e.getArchivedAt()).archivedBy(e.getArchivedBy());
        }
        return b.build();
    }

    public UomCategoryEntity categoryDomainToEntity(UomCategory c, UomCategoryEntity existingOrNull) {
        if (c == null) return null;
        UomCategoryEntity e = existingOrNull != null ? existingOrNull : new UomCategoryEntity();
        e.setId(c.getId().getId());
        e.setCompanyId(c.getCompanyId().getId());
        e.setName(c.getName());
        e.setActive(c.isActive());
        e.setArchivedAt(c.getArchivedAt());
        e.setArchivedBy(c.getArchivedBy());
        return e;
    }

    public UnitOfMeasure entityToDomain(UomEntity e) {
        if (e == null) return null;
        UnitOfMeasure.Builder b = UnitOfMeasure.builder()
                .id(new UomId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .categoryId(new UomCategoryId(e.getCategoryId()))
                .name(e.getName())
                .uomType(e.getUomType())
                .factor(e.getFactor())
                .rounding(e.getRounding());
        if (!e.isActive()) {
            b.archived(true).archivedAt(e.getArchivedAt()).archivedBy(e.getArchivedBy());
        }
        return b.build();
    }

    public UomEntity domainToEntity(UnitOfMeasure u, UomEntity existingOrNull) {
        if (u == null) return null;
        UomEntity e = existingOrNull != null ? existingOrNull : new UomEntity();
        e.setId(u.getId().getId());
        e.setCompanyId(u.getCompanyId().getId());
        e.setCategoryId(u.getCategoryId().getId());
        e.setName(u.getName());
        e.setUomType(u.getUomType());
        e.setFactor(u.getFactor());
        e.setRounding(u.getRounding());
        e.setActive(u.isActive());
        e.setArchivedAt(u.getArchivedAt());
        e.setArchivedBy(u.getArchivedBy());
        return e;
    }
}
