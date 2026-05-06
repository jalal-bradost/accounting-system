package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCategoryCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCategoryResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomResponse;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface UomApplicationService {

    UomCategoryResponse createUomCategory(@Valid UomCategoryCommand command);
    List<UomCategoryResponse> listUomCategories(CompanyId companyId, boolean includeArchived);

    UomResponse createUom(@Valid UomCommand command);
    UomResponse updateUom(UUID uomId, @Valid UomCommand command);
    UomResponse getUom(UUID uomId);
    List<UomResponse> listUomsByCategory(UUID categoryId, boolean includeArchived);

    /** Convert {@code qty} expressed in {@code fromUomId} to the equivalent in {@code toUomId}. */
    BigDecimal convert(UUID fromUomId, UUID toUomId, BigDecimal quantity);
}
