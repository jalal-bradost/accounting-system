package com.bradox.erp.bootstrap;

import com.bradox.erp.dataaccess.entity.AccountEntity;
import com.bradox.erp.dataaccess.entity.JournalEntity;
import com.bradox.erp.dataaccess.repository.AccountJpaRepository;
import com.bradox.erp.dataaccess.repository.JournalJpaRepository;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.JournalType;
import com.bradox.erp.inventory.dataaccess.entity.ProductCategoryEntity;
import com.bradox.erp.inventory.dataaccess.entity.StockLocationEntity;
import com.bradox.erp.inventory.dataaccess.entity.UomCategoryEntity;
import com.bradox.erp.inventory.dataaccess.entity.UomEntity;
import com.bradox.erp.inventory.dataaccess.entity.WarehouseEntity;
import com.bradox.erp.inventory.dataaccess.repository.ProductCategoryJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.StockLocationJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.UomCategoryJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.UomJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.WarehouseJpaRepository;
import com.bradox.erp.inventory.domain.core.valueobject.LocationType;
import com.bradox.erp.inventory.domain.core.valueobject.UomType;
import com.bradox.erp.inventory.domain.core.valueobject.ValuationMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Idempotent ERP defaults for any company: chart of accounts, journals, UoMs,
 * default warehouse/locations, and product category inventory accounts.
 */
@Service
public class CompanyErpBootstrap {

    public static final String DEFAULT_WAREHOUSE_CODE = "WH";
    public static final String OPENING_BALANCE_EQUITY_CODE = "430019";
    public static final String INVENTORY_ACCOUNT_CODE = "430010";

    private final AccountJpaRepository accountRepository;
    private final JournalJpaRepository journalRepository;
    private final UomCategoryJpaRepository uomCategoryRepository;
    private final UomJpaRepository uomRepository;
    private final WarehouseJpaRepository warehouseRepository;
    private final StockLocationJpaRepository locationRepository;
    private final ProductCategoryJpaRepository productCategoryRepository;

    public CompanyErpBootstrap(AccountJpaRepository accountRepository,
                               JournalJpaRepository journalRepository,
                               UomCategoryJpaRepository uomCategoryRepository,
                               UomJpaRepository uomRepository,
                               WarehouseJpaRepository warehouseRepository,
                               StockLocationJpaRepository locationRepository,
                               ProductCategoryJpaRepository productCategoryRepository) {
        this.accountRepository = accountRepository;
        this.journalRepository = journalRepository;
        this.uomCategoryRepository = uomCategoryRepository;
        this.uomRepository = uomRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    @Transactional
    public void bootstrap(UUID companyId) {
        seedAccounts(companyId);
        seedJournals(companyId);
        seedUomCategoriesAndUnits(companyId);
        seedWarehouseAndLocations(companyId);
        seedDefaultProductCategory(companyId);
    }

    private void seedAccounts(UUID companyId) {
        insertAccount(companyId, "430001", "Cash", AccountType.BANK_AND_CASH);
        insertAccount(companyId, "430002", "Bank", AccountType.BANK_AND_CASH);
        insertAccount(companyId, "430003", "Accounts Receivable", AccountType.RECEIVABLE);
        insertAccount(companyId, "430004", "Accounts Payable", AccountType.PAYABLE);
        insertAccount(companyId, "430005", "Sales Revenue", AccountType.INCOME);
        insertAccount(companyId, "430006", "Sales Discount", AccountType.EXPENSES);
        insertAccount(companyId, "430007", "Purchase Discount", AccountType.OTHER_INCOME);
        insertAccount(companyId, "430008", "Gift Expense", AccountType.EXPENSES);
        insertAccount(companyId, "430009", "Cost of goods sold", AccountType.COST_OF_REVENUE);
        insertAccount(companyId, "430010", "Inventory", AccountType.CURRENT_ASSETS);
        insertAccount(companyId, "430011", "Stock Input (GR/IR)", AccountType.CURRENT_ASSETS);
        insertAccount(companyId, "430012", "Stock Output", AccountType.CURRENT_ASSETS);
        insertAccount(companyId, "430013", "Purchase VAT", AccountType.CURRENT_ASSETS);
        insertAccount(companyId, "430014", "Exchange Gain", AccountType.OTHER_INCOME);
        insertAccount(companyId, "430015", "Exchange Loss", AccountType.OTHER_EXPENSES);
        insertAccount(companyId, "430016", "Salary Expense", AccountType.EXPENSES);
        insertAccount(companyId, "430017", "Salaries Payable", AccountType.CURRENT_LIABILITIES);
        insertAccount(companyId, "430018", "Payroll Deductions Payable", AccountType.CURRENT_LIABILITIES);
        insertAccount(companyId, "430019", "Opening Balance Equity", AccountType.EQUITY);
        insertAccount(companyId, "430020", "Owner Capital", AccountType.EQUITY);
        insertAccount(companyId, "430021", "Expenses", AccountType.EXPENSES);
        insertAccount(companyId, "430025", "Opening Balance Adjustment", AccountType.EQUITY);
        insertAccount(companyId, "430026", "Purchase Price Variance", AccountType.EXPENSES);
    }

    private void insertAccount(UUID companyId, String code, String name, AccountType type) {
        if (accountRepository.existsByCompanyIdAndCode(companyId, code)) {
            return;
        }
        AccountEntity e = new AccountEntity();
        e.setId(UUID.randomUUID());
        e.setCompanyId(companyId);
        e.setCode(code);
        e.setName(name);
        e.setType(type);
        e.setActive(true);
        accountRepository.save(e);
    }

    private void seedJournals(UUID companyId) {
        insertJournal(companyId, "430001", "Cash", JournalType.CASH);
        insertJournal(companyId, "430002", "Bank", JournalType.BANK);
        insertJournal(companyId, "430003", "Sale", JournalType.SALE);
        insertJournal(companyId, "430004", "Purchase", JournalType.PURCHASE);
        insertJournal(companyId, "INV", "Inventory Valuation", JournalType.MISC);
        insertJournal(companyId, "PAY", "Payroll", JournalType.MISC);
        insertJournal(companyId, "OPEN", "Opening Balances", JournalType.MISC);
        insertJournal(companyId, "EXP", "Expense", JournalType.MISC);
        insertJournal(companyId, "COMM", "Commissions", JournalType.MISC);
        insertJournal(companyId, "EXCH", "Exchange Difference", JournalType.MISC);
    }

    private void insertJournal(UUID companyId, String code, String name, JournalType type) {
        if (journalRepository.existsByCompanyIdAndCode(companyId, code)) {
            return;
        }
        JournalEntity e = new JournalEntity();
        e.setId(UUID.randomUUID());
        e.setCompanyId(companyId);
        e.setCode(code);
        e.setName(name);
        e.setType(type);
        journalRepository.save(e);
    }

    private void seedUomCategoriesAndUnits(UUID companyId) {
        UomCategoryEntity units = ensureUomCategory(companyId, "Units");
        ensureUom(companyId, units, "Unit", UomType.REFERENCE, BigDecimal.ONE, 0);
        ensureUom(companyId, units, "Dozen", UomType.BIGGER, new BigDecimal("12"), 0);
        ensureUom(companyId, units, "Pack of 100", UomType.BIGGER, new BigDecimal("100"), 0);

        UomCategoryEntity weight = ensureUomCategory(companyId, "Weight");
        ensureUom(companyId, weight, "kg", UomType.REFERENCE, BigDecimal.ONE, 3);
        ensureUom(companyId, weight, "g", UomType.SMALLER, new BigDecimal("0.001"), 3);
        ensureUom(companyId, weight, "tonne", UomType.BIGGER, new BigDecimal("1000"), 3);
        ensureUom(companyId, weight, "lb", UomType.SMALLER, new BigDecimal("0.4535924"), 3);

        UomCategoryEntity length = ensureUomCategory(companyId, "Length");
        ensureUom(companyId, length, "m", UomType.REFERENCE, BigDecimal.ONE, 3);
        ensureUom(companyId, length, "cm", UomType.SMALLER, new BigDecimal("0.01"), 3);
        ensureUom(companyId, length, "mm", UomType.SMALLER, new BigDecimal("0.001"), 3);
        ensureUom(companyId, length, "km", UomType.BIGGER, new BigDecimal("1000"), 3);

        UomCategoryEntity time = ensureUomCategory(companyId, "Time");
        ensureUom(companyId, time, "Hours", UomType.REFERENCE, BigDecimal.ONE, 2);
        ensureUom(companyId, time, "Days", UomType.BIGGER, new BigDecimal("8"), 2);
        ensureUom(companyId, time, "Minutes", UomType.SMALLER, new BigDecimal("0.0166667"), 2);
    }

    private UomCategoryEntity ensureUomCategory(UUID companyId, String name) {
        return uomCategoryRepository.findByCompany(companyId, true).stream()
                .filter(c -> name.equalsIgnoreCase(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    UomCategoryEntity c = new UomCategoryEntity();
                    c.setId(UUID.randomUUID());
                    c.setCompanyId(companyId);
                    c.setName(name);
                    c.setActive(true);
                    return uomCategoryRepository.save(c);
                });
    }

    private void ensureUom(UUID companyId, UomCategoryEntity category, String name,
                           UomType type, BigDecimal factor, int rounding) {
        boolean exists = uomRepository.findByCategory(category.getId(), true).stream()
                .anyMatch(u -> name.equalsIgnoreCase(u.getName()));
        if (exists) {
            return;
        }
        UomEntity u = new UomEntity();
        u.setId(UUID.randomUUID());
        u.setCompanyId(companyId);
        u.setCategoryId(category.getId());
        u.setName(name);
        u.setUomType(type);
        u.setFactor(factor);
        u.setRounding(rounding);
        u.setActive(true);
        uomRepository.save(u);
    }

    private void seedWarehouseAndLocations(UUID companyId) {
        WarehouseEntity warehouse = warehouseRepository.findByCompany(companyId, true).stream()
                .filter(w -> DEFAULT_WAREHOUSE_CODE.equalsIgnoreCase(w.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    WarehouseEntity w = new WarehouseEntity();
                    w.setId(UUID.randomUUID());
                    w.setCompanyId(companyId);
                    w.setCode(DEFAULT_WAREHOUSE_CODE);
                    w.setName("Main Warehouse");
                    w.setActive(true);
                    return warehouseRepository.save(w);
                });

        StockLocationEntity stock = ensureLocation(companyId, warehouse.getId(),
                DEFAULT_WAREHOUSE_CODE + "/STOCK", "Main Warehouse / Stock", LocationType.INTERNAL);
        warehouse.setStockLocationId(stock.getId());
        warehouse.setInputLocationId(null);
        warehouse.setOutputLocationId(null);
        warehouseRepository.save(warehouse);

        deactivateLocationByCode(companyId, DEFAULT_WAREHOUSE_CODE + "/Quarantine");

        ensureLocation(companyId, null, "VIRT/SUPPLIERS", "Virtual / Suppliers", LocationType.SUPPLIER);
        ensureLocation(companyId, null, "VIRT/CUSTOMERS", "Virtual / Customers", LocationType.CUSTOMER);
        ensureLocation(companyId, null, "VIRT/INVENTORY-LOSS", "Virtual / Inventory Loss", LocationType.INVENTORY_LOSS);
    }

    private StockLocationEntity ensureLocation(UUID companyId, UUID warehouseId, String code,
                                               String name, LocationType type) {
        return locationRepository.findByCompany(companyId, true).stream()
                .filter(l -> code.equalsIgnoreCase(l.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    StockLocationEntity l = new StockLocationEntity();
                    l.setId(UUID.randomUUID());
                    l.setCompanyId(companyId);
                    l.setCode(code);
                    l.setName(name);
                    l.setLocationType(type);
                    l.setWarehouseId(warehouseId);
                    l.setAllowNegativeStock(false);
                    l.setActive(true);
                    return locationRepository.save(l);
                });
    }

    private void deactivateLocationByCode(UUID companyId, String code) {
        locationRepository.findByCompany(companyId, true).stream()
                .filter(l -> code.equalsIgnoreCase(l.getCode()))
                .filter(StockLocationEntity::isActive)
                .findFirst()
                .ifPresent(l -> {
                    l.setActive(false);
                    locationRepository.save(l);
                });
    }

    private void seedDefaultProductCategory(UUID companyId) {
        Map<String, UUID> accountIdsByCode = new HashMap<>();
        for (AccountEntity a : accountRepository.findByCompanyId(companyId)) {
            accountIdsByCode.put(a.getCode(), a.getId());
        }
        UUID valuation = accountIdsByCode.get("430010");
        UUID input = accountIdsByCode.get("430011");
        UUID output = accountIdsByCode.get("430012");
        UUID cogs = accountIdsByCode.get("430009");

        ProductCategoryEntity all = productCategoryRepository.findByCompany(companyId, true).stream()
                .filter(c -> "All".equalsIgnoreCase(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    ProductCategoryEntity c = new ProductCategoryEntity();
                    c.setId(UUID.randomUUID());
                    c.setCompanyId(companyId);
                    c.setName("All");
                    c.setValuationMethod(ValuationMethod.AVCO);
                    c.setActive(true);
                    return c;
                });
        applyInventoryAccounts(all, valuation, input, output, cogs);
        productCategoryRepository.save(all);

        for (ProductCategoryEntity c : productCategoryRepository.findByCompany(companyId, true)) {
            if (c.getStockValuationAccountId() != null
                    && c.getStockInputAccountId() != null
                    && c.getStockOutputAccountId() != null
                    && c.getCogsAccountId() != null) {
                continue;
            }
            applyInventoryAccounts(c, valuation, input, output, cogs);
            productCategoryRepository.save(c);
        }
    }

    private static void applyInventoryAccounts(ProductCategoryEntity c,
                                               UUID valuation, UUID input, UUID output, UUID cogs) {
        if (c.getStockValuationAccountId() == null) {
            c.setStockValuationAccountId(valuation);
        }
        if (c.getStockInputAccountId() == null) {
            c.setStockInputAccountId(input);
        }
        if (c.getStockOutputAccountId() == null) {
            c.setStockOutputAccountId(output);
        }
        if (c.getCogsAccountId() == null) {
            c.setCogsAccountId(cogs);
        }
    }
}
