package com.jalaldeveloper.accountingsystem.bootstrap;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccountEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccountJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.ProductCategoryEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockLocationEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.UomCategoryEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.UomEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.WarehouseEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.ProductCategoryJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.StockLocationJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.UomCategoryJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.UomJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.WarehouseJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;
import com.jalaldeveloper.accountingsystem.platform.bootstrap.PlatformRbacSeeder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bootstraps the inventory module for the demo company:
 * <ul>
 *   <li>UoM categories (Units, Weight, Length, Time) with reference + a few common units.</li>
 *   <li>Default warehouse {@code WH} with auto-provisioned Stock / Input / Output internal locations.</li>
 *   <li>A virtual {@link LocationType#INVENTORY_LOSS} location (used by stock adjustments).</li>
 *   <li>One default product category wired to the seeded inventory accounts.</li>
 * </ul>
 *
 * Runs idempotently after {@link DefaultCompanyChartDataSeeder} so that account ids
 * are available for category wiring.
 */
@Component
@Order(2)
@ConditionalOnProperty(
        name = "inventory.seed.default-company",
        havingValue = "true",
        matchIfMissing = true)
public class DefaultInventorySeeder implements ApplicationRunner {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;
    private static final String DEFAULT_WAREHOUSE_CODE = "WH";

    private final AccountJpaRepository accountRepository;
    private final UomCategoryJpaRepository uomCategoryRepository;
    private final UomJpaRepository uomRepository;
    private final WarehouseJpaRepository warehouseRepository;
    private final StockLocationJpaRepository locationRepository;
    private final ProductCategoryJpaRepository productCategoryRepository;

    public DefaultInventorySeeder(AccountJpaRepository accountRepository,
                                  UomCategoryJpaRepository uomCategoryRepository,
                                  UomJpaRepository uomRepository,
                                  WarehouseJpaRepository warehouseRepository,
                                  StockLocationJpaRepository locationRepository,
                                  ProductCategoryJpaRepository productCategoryRepository) {
        this.accountRepository = accountRepository;
        this.uomCategoryRepository = uomCategoryRepository;
        this.uomRepository = uomRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUomCategoriesAndUnits();
        seedWarehouseAndLocations();
        seedDefaultProductCategory();
    }

    private void seedUomCategoriesAndUnits() {
        UomCategoryEntity units = ensureUomCategory("Units");
        ensureUom(units, "Unit", UomType.REFERENCE, BigDecimal.ONE, 0);
        ensureUom(units, "Dozen", UomType.BIGGER, new BigDecimal("12"), 0);
        ensureUom(units, "Pack of 100", UomType.BIGGER, new BigDecimal("100"), 0);

        UomCategoryEntity weight = ensureUomCategory("Weight");
        ensureUom(weight, "kg", UomType.REFERENCE, BigDecimal.ONE, 3);
        ensureUom(weight, "g", UomType.SMALLER, new BigDecimal("0.001"), 3);
        ensureUom(weight, "tonne", UomType.BIGGER, new BigDecimal("1000"), 3);
        ensureUom(weight, "lb", UomType.SMALLER, new BigDecimal("0.4535924"), 3);

        UomCategoryEntity length = ensureUomCategory("Length");
        ensureUom(length, "m", UomType.REFERENCE, BigDecimal.ONE, 3);
        ensureUom(length, "cm", UomType.SMALLER, new BigDecimal("0.01"), 3);
        ensureUom(length, "mm", UomType.SMALLER, new BigDecimal("0.001"), 3);
        ensureUom(length, "km", UomType.BIGGER, new BigDecimal("1000"), 3);

        UomCategoryEntity time = ensureUomCategory("Time");
        ensureUom(time, "Hours", UomType.REFERENCE, BigDecimal.ONE, 2);
        ensureUom(time, "Days", UomType.BIGGER, new BigDecimal("8"), 2);
        ensureUom(time, "Minutes", UomType.SMALLER, new BigDecimal("0.0166667"), 2);
    }

    private UomCategoryEntity ensureUomCategory(String name) {
        return uomCategoryRepository.findByCompany(COMPANY_ID, true).stream()
                .filter(c -> name.equalsIgnoreCase(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    UomCategoryEntity c = new UomCategoryEntity();
                    c.setId(UUID.randomUUID());
                    c.setCompanyId(COMPANY_ID);
                    c.setName(name);
                    c.setActive(true);
                    return uomCategoryRepository.save(c);
                });
    }

    private void ensureUom(UomCategoryEntity category, String name, UomType type, BigDecimal factor, int rounding) {
        boolean exists = uomRepository.findByCategory(category.getId(), true).stream()
                .anyMatch(u -> name.equalsIgnoreCase(u.getName()));
        if (exists) return;
        UomEntity u = new UomEntity();
        u.setId(UUID.randomUUID());
        u.setCompanyId(COMPANY_ID);
        u.setCategoryId(category.getId());
        u.setName(name);
        u.setUomType(type);
        u.setFactor(factor);
        u.setRounding(rounding);
        u.setActive(true);
        uomRepository.save(u);
    }

    private void seedWarehouseAndLocations() {
        WarehouseEntity warehouse = warehouseRepository.findByCompany(COMPANY_ID, true).stream()
                .filter(w -> DEFAULT_WAREHOUSE_CODE.equalsIgnoreCase(w.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    WarehouseEntity w = new WarehouseEntity();
                    w.setId(UUID.randomUUID());
                    w.setCompanyId(COMPANY_ID);
                    w.setCode(DEFAULT_WAREHOUSE_CODE);
                    w.setName("Main Warehouse");
                    w.setActive(true);
                    return warehouseRepository.save(w);
                });

        // Auto-provision Stock / Input / Output internal locations and link them.
        StockLocationEntity stock = ensureLocation(warehouse.getId(),
                DEFAULT_WAREHOUSE_CODE + "/STOCK", "Main Warehouse / Stock", LocationType.INTERNAL);
        StockLocationEntity input = ensureLocation(warehouse.getId(),
                DEFAULT_WAREHOUSE_CODE + "/INPUT", "Main Warehouse / Input", LocationType.INTERNAL);
        StockLocationEntity output = ensureLocation(warehouse.getId(),
                DEFAULT_WAREHOUSE_CODE + "/OUTPUT", "Main Warehouse / Output", LocationType.INTERNAL);
        warehouse.setStockLocationId(stock.getId());
        warehouse.setInputLocationId(input.getId());
        warehouse.setOutputLocationId(output.getId());
        warehouseRepository.save(warehouse);

        // Virtual counterparties used by adjustments / external moves.
        ensureLocation(null, "VIRT/SUPPLIERS", "Virtual / Suppliers", LocationType.SUPPLIER);
        ensureLocation(null, "VIRT/CUSTOMERS", "Virtual / Customers", LocationType.CUSTOMER);
        ensureLocation(null, "VIRT/INVENTORY-LOSS", "Virtual / Inventory Loss", LocationType.INVENTORY_LOSS);
    }

    private StockLocationEntity ensureLocation(UUID warehouseId, String code, String name, LocationType type) {
        return locationRepository.findByCompany(COMPANY_ID, true).stream()
                .filter(l -> code.equalsIgnoreCase(l.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    StockLocationEntity l = new StockLocationEntity();
                    l.setId(UUID.randomUUID());
                    l.setCompanyId(COMPANY_ID);
                    l.setCode(code);
                    l.setName(name);
                    l.setLocationType(type);
                    l.setWarehouseId(warehouseId);
                    l.setAllowNegativeStock(false);
                    l.setActive(true);
                    return locationRepository.save(l);
                });
    }

    private void seedDefaultProductCategory() {
        boolean exists = productCategoryRepository.findByCompany(COMPANY_ID, true).stream()
                .anyMatch(c -> "All".equalsIgnoreCase(c.getName()));
        if (exists) return;
        Map<String, UUID> accountIdsByCode = new HashMap<>();
        for (AccountEntity a : accountRepository.findByCompanyId(COMPANY_ID)) {
            accountIdsByCode.put(a.getCode(), a.getId());
        }
        ProductCategoryEntity c = new ProductCategoryEntity();
        c.setId(UUID.randomUUID());
        c.setCompanyId(COMPANY_ID);
        c.setName("All");
        c.setValuationMethod(ValuationMethod.AVCO);
        // 430010=Inventory, 430011=Stock Input, 430012=Stock Output, 430009=COGS
        c.setStockValuationAccountId(accountIdsByCode.get("430010"));
        c.setStockInputAccountId(accountIdsByCode.get("430011"));
        c.setStockOutputAccountId(accountIdsByCode.get("430012"));
        c.setCogsAccountId(accountIdsByCode.get("430009"));
        c.setActive(true);
        productCategoryRepository.save(c);
    }
}
