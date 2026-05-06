package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(ProductId id);
    Optional<Product> findByIdIncludingArchived(ProductId id);
    Page<Product> search(CompanyId companyId, String query, boolean includeArchived, Pageable pageable);
}
