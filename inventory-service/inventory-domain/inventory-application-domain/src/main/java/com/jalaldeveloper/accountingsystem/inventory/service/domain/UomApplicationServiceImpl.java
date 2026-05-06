package com.jalaldeveloper.accountingsystem.inventory.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UnitOfMeasure;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.UomCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCategoryCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCategoryResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.UomResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.mapper.InventoryDataMapper;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.UomApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.UomCategoryRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.UomRepository;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Validated
class UomApplicationServiceImpl implements UomApplicationService {

    private final UomRepository uomRepository;
    private final UomCategoryRepository uomCategoryRepository;
    private final InventoryDataMapper mapper;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    UomApplicationServiceImpl(UomRepository uomRepository,
                              UomCategoryRepository uomCategoryRepository,
                              InventoryDataMapper mapper,
                              ObjectProvider<CompanyContext> companyContextProvider) {
        this.uomRepository = uomRepository;
        this.uomCategoryRepository = uomCategoryRepository;
        this.mapper = mapper;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional
    public UomCategoryResponse createUomCategory(UomCategoryCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        UomCategory entity = mapper.uomCategoryCommandToDomain(command, UUID.randomUUID(), companyId);
        entity.validate();
        return mapper.uomCategoryToResponse(uomCategoryRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UomCategoryResponse> listUomCategories(CompanyId companyId, boolean includeArchived) {
        return uomCategoryRepository.findByCompany(companyId, includeArchived).stream()
                .map(mapper::uomCategoryToResponse)
                .toList();
    }

    @Override
    @Transactional
    public UomResponse createUom(UomCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        UnitOfMeasure entity = mapper.uomCommandToDomain(command, UUID.randomUUID(), companyId);
        entity.validate();
        return mapper.uomToResponse(uomRepository.save(entity));
    }

    @Override
    @Transactional
    public UomResponse updateUom(UUID uomId, UomCommand command) {
        UnitOfMeasure existing = uomRepository.findById(new UomId(uomId))
                .orElseThrow(() -> new InventoryDomainException("UoM not found: " + uomId));
        if (command.getName() != null) existing.rename(command.getName());
        if (command.getFactor() != null) existing.changeFactor(command.getFactor());
        if (command.getRounding() != null) existing.changeRounding(command.getRounding());
        existing.validate();
        return mapper.uomToResponse(uomRepository.save(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public UomResponse getUom(UUID uomId) {
        return uomRepository.findByIdIncludingArchived(new UomId(uomId))
                .map(mapper::uomToResponse)
                .orElseThrow(() -> new InventoryDomainException("UoM not found: " + uomId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UomResponse> listUomsByCategory(UUID categoryId, boolean includeArchived) {
        return uomRepository.findByCategory(new UomCategoryId(categoryId), includeArchived).stream()
                .map(mapper::uomToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal convert(UUID fromUomId, UUID toUomId, BigDecimal quantity) {
        UnitOfMeasure from = uomRepository.findById(new UomId(fromUomId))
                .orElseThrow(() -> new InventoryDomainException("UoM not found: " + fromUomId));
        UnitOfMeasure to = uomRepository.findById(new UomId(toUomId))
                .orElseThrow(() -> new InventoryDomainException("UoM not found: " + toUomId));
        return from.convertTo(quantity, to);
    }

    private CompanyId resolveCompany(UUID explicit) {
        if (explicit != null) return new CompanyId(explicit);
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) {
            return ctx.currentCompany().orElseThrow(() ->
                    new IllegalArgumentException("companyId required (header X-Company-Id, query param, or body)"));
        }
        throw new IllegalArgumentException("companyId required");
    }
}
