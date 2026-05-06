package com.jalaldeveloper.accountingsystem.inventory.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockLocation;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Warehouse;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockLocationCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockLocationResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.WarehouseCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.WarehouseResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.mapper.InventoryDataMapper;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.WarehouseApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockLocationRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.WarehouseRepository;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
class WarehouseApplicationServiceImpl implements WarehouseApplicationService {

    private final WarehouseRepository warehouseRepository;
    private final StockLocationRepository locationRepository;
    private final InventoryDataMapper mapper;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    WarehouseApplicationServiceImpl(WarehouseRepository warehouseRepository,
                                    StockLocationRepository locationRepository,
                                    InventoryDataMapper mapper,
                                    ObjectProvider<CompanyContext> companyContextProvider) {
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.mapper = mapper;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(WarehouseCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        Warehouse warehouse = mapper.warehouseCommandToDomain(command, UUID.randomUUID(), companyId);
        warehouse.validate();
        Warehouse savedWh = warehouseRepository.save(warehouse);

        // Auto-provision Stock/Input/Output locations.
        StockLocation stock = StockLocation.builder()
                .id(new StockLocationId(UUID.randomUUID()))
                .companyId(companyId)
                .code(savedWh.getCode() + "/STOCK")
                .name(savedWh.getName() + " / Stock")
                .locationType(LocationType.INTERNAL)
                .warehouseId(savedWh.getId())
                .build();
        StockLocation input = StockLocation.builder()
                .id(new StockLocationId(UUID.randomUUID()))
                .companyId(companyId)
                .code(savedWh.getCode() + "/INPUT")
                .name(savedWh.getName() + " / Input")
                .locationType(LocationType.INTERNAL)
                .warehouseId(savedWh.getId())
                .build();
        StockLocation output = StockLocation.builder()
                .id(new StockLocationId(UUID.randomUUID()))
                .companyId(companyId)
                .code(savedWh.getCode() + "/OUTPUT")
                .name(savedWh.getName() + " / Output")
                .locationType(LocationType.INTERNAL)
                .warehouseId(savedWh.getId())
                .build();
        stock.validate();
        input.validate();
        output.validate();
        StockLocation savedStock = locationRepository.save(stock);
        StockLocation savedInput = locationRepository.save(input);
        StockLocation savedOutput = locationRepository.save(output);
        savedWh.linkStockLocation(savedStock.getId());
        savedWh.linkInputLocation(savedInput.getId());
        savedWh.linkOutputLocation(savedOutput.getId());
        return mapper.warehouseToResponse(warehouseRepository.save(savedWh));
    }

    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(UUID warehouseId, WarehouseCommand command) {
        Warehouse w = warehouseRepository.findById(new WarehouseId(warehouseId))
                .orElseThrow(() -> new InventoryDomainException("Warehouse not found: " + warehouseId));
        if (command.getName() != null) w.rename(command.getName());
        w.validate();
        return mapper.warehouseToResponse(warehouseRepository.save(w));
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouse(UUID warehouseId) {
        return warehouseRepository.findByIdIncludingArchived(new WarehouseId(warehouseId))
                .map(mapper::warehouseToResponse)
                .orElseThrow(() -> new InventoryDomainException("Warehouse not found: " + warehouseId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> listWarehouses(CompanyId companyId, boolean includeArchived) {
        return warehouseRepository.findByCompany(companyId, includeArchived).stream()
                .map(mapper::warehouseToResponse)
                .toList();
    }

    @Override
    @Transactional
    public StockLocationResponse createLocation(StockLocationCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        StockLocation entity = mapper.locationCommandToDomain(command, UUID.randomUUID(), companyId);
        entity.validate();
        return mapper.locationToResponse(locationRepository.save(entity));
    }

    @Override
    @Transactional
    public StockLocationResponse updateLocation(UUID locationId, StockLocationCommand command) {
        StockLocation l = locationRepository.findById(new StockLocationId(locationId))
                .orElseThrow(() -> new InventoryDomainException("Location not found: " + locationId));
        if (command.getName() != null) l.rename(command.getName());
        l.changeAllowNegative(command.isAllowNegativeStock());
        l.validate();
        return mapper.locationToResponse(locationRepository.save(l));
    }

    @Override
    @Transactional(readOnly = true)
    public StockLocationResponse getLocation(UUID locationId) {
        return locationRepository.findByIdIncludingArchived(new StockLocationId(locationId))
                .map(mapper::locationToResponse)
                .orElseThrow(() -> new InventoryDomainException("Location not found: " + locationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockLocationResponse> listLocations(CompanyId companyId, boolean includeArchived) {
        return locationRepository.findByCompany(companyId, includeArchived).stream()
                .map(mapper::locationToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockLocationResponse> listLocationsByWarehouse(UUID warehouseId, boolean includeArchived) {
        return locationRepository.findByWarehouse(new WarehouseId(warehouseId), includeArchived).stream()
                .map(mapper::locationToResponse)
                .toList();
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
