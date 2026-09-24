package com.bradox.delin.inventory.dataaccess.mapper;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.domain.valueobject.Money;
import com.bradox.delin.inventory.dataaccess.entity.ProductPackagingEntity;
import com.bradox.delin.inventory.domain.core.entity.ProductPackaging;
import com.bradox.delin.inventory.domain.core.valueobject.ProductId;
import com.bradox.delin.inventory.domain.core.valueobject.ProductPackagingId;
import org.springframework.stereotype.Component;

@Component
public class ProductPackagingDataAccessMapper {

    public ProductPackaging entityToDomain(ProductPackagingEntity e) {
        if (e == null) return null;
        ProductPackaging d = new ProductPackaging();
        d.setId(new ProductPackagingId(e.getId()));
        d.setCompanyId(new CompanyId(e.getCompanyId()));
        d.setProductId(new ProductId(e.getProductId()));
        d.setName(e.getName());
        d.setQty(e.getQty());
        d.setPurchasePrice(new Money(e.getPurchasePrice()));
        d.setListPrice(new Money(e.getListPrice()));
        d.setBarcode(e.getBarcode());
        d.setSku(e.getSku());
        d.setActive(e.isActive());
        d.setBase(e.isBase());
        if (e.getPackagedProductId() != null) {
            d.setPackagedProductId(new ProductId(e.getPackagedProductId()));
        }
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    public ProductPackagingEntity domainToEntity(ProductPackaging d) {
        ProductPackagingEntity e = new ProductPackagingEntity();
        updateEntity(e, d);
        return e;
    }

    public void updateEntity(ProductPackagingEntity e, ProductPackaging d) {
        e.setId(d.getId().getId());
        e.setCompanyId(d.getCompanyId().getId());
        e.setProductId(d.getProductId().getId());
        e.setName(d.getName());
        e.setQty(d.getQty());
        e.setPurchasePrice(d.getPurchasePrice() != null ? d.getPurchasePrice().getAmount() : java.math.BigDecimal.ZERO);
        e.setListPrice(d.getListPrice() != null ? d.getListPrice().getAmount() : java.math.BigDecimal.ZERO);
        e.setBarcode(d.getBarcode());
        e.setSku(d.getSku());
        e.setActive(d.isActive());
        e.setBase(d.isBase());
        e.setPackagedProductId(d.getPackagedProductId() != null ? d.getPackagedProductId().getId() : null);
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
    }
}
