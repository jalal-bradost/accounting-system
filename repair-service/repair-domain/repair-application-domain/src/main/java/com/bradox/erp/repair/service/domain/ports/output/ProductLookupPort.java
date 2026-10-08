package com.bradox.erp.repair.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Reads a product's name and list price from Inventory (implemented in infrastructure). */
public interface ProductLookupPort {

    record Product(UUID id, String name, BigDecimal listPrice) {
    }

    Optional<Product> find(CompanyId companyId, UUID productId);
}
