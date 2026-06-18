package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateProductCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UpdateProductCommand;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductApplicationService {

    ProductResponse createProduct(@Valid CreateProductCommand command);

    ProductResponse updateProduct(UUID productId, @Valid UpdateProductCommand command);

    ProductResponse uploadProductImage(UUID productId, MultipartFile file);

    ProductResponse deleteProductImage(UUID productId);

    ProductResponse archiveProduct(UUID productId);

    ProductResponse unarchiveProduct(UUID productId);

    ProductResponse getProduct(UUID productId);

    Page<ProductResponse> searchProducts(CompanyId companyId,
                                         String query,
                                         boolean includeArchived,
                                         Pageable pageable);

    ProductCategoryResponse createCategory(@Valid ProductCategoryCommand command);

    ProductCategoryResponse updateCategory(UUID categoryId, @Valid ProductCategoryCommand command);

    List<ProductCategoryResponse> listCategories(CompanyId companyId, boolean includeArchived);
}
