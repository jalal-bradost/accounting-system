package com.jalaldeveloper.accountingsystem.importdata;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerKind;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.CreatePartnerCommand;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PartnerResponse;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PartnerApplicationService;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccountEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.DatasetImportRefEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccountJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.DatasetImportRefJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.UomEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.WarehouseEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.UomJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.WarehouseJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateProductCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductCategoryResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ValidatePickingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.ProductApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.jalaldeveloper.accountingsystem.pos.domain.core.PosPaymentMethod;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.CheckoutPosOrderCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.ClosePosSessionCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.OpenPosSessionCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosConfigCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosConfigResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosOrderLineCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosSessionResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.RegisterPosPaymentCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.ports.input.PosApplicationService;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.FiscalTaxScope;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.TaxAmountType;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.CreateFiscalTaxCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.CreatePurchaseOrderCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.CreateVendorBillFromPoCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.FiscalTaxResponse;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.PurchaseOrderLineCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.PurchaseOrderResponse;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.RegisterVendorPaymentCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.VendorBillResponse;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderLineCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.input.SalesApplicationService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DatasetImportService {

    static final String PARTNER = "PARTNER";
    static final String CATEGORY = "CATEGORY";
    static final String PRODUCT = "PRODUCT";
    static final String TAX = "TAX";
    static final String PO = "PO";
    static final String SO = "SO";
    static final String POS_SESSION = "POS_SESSION";
    static final String POS_CONFIG = "POS_CONFIG";

    private final ProductApplicationService productApplicationService;
    private final PartnerApplicationService partnerApplicationService;
    private final PurchaseApplicationService purchaseApplicationService;
    private final SalesApplicationService salesApplicationService;
    private final CustomerInvoiceApplicationService customerInvoiceApplicationService;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final StockPickingApplicationService stockPickingApplicationService;
    private final PosApplicationService posApplicationService;
    private final AccountJpaRepository accountJpaRepository;
    private final JournalJpaRepository journalJpaRepository;
    private final UomJpaRepository uomJpaRepository;
    private final WarehouseJpaRepository warehouseJpaRepository;
    private final DatasetImportRefJpaRepository refRepository;

    public DatasetImportService(ProductApplicationService productApplicationService,
                                PartnerApplicationService partnerApplicationService,
                                PurchaseApplicationService purchaseApplicationService,
                                SalesApplicationService salesApplicationService,
                                CustomerInvoiceApplicationService customerInvoiceApplicationService,
                                JournalEntryApplicationService journalEntryApplicationService,
                                StockPickingApplicationService stockPickingApplicationService,
                                PosApplicationService posApplicationService,
                                AccountJpaRepository accountJpaRepository,
                                JournalJpaRepository journalJpaRepository,
                                UomJpaRepository uomJpaRepository,
                                WarehouseJpaRepository warehouseJpaRepository,
                                DatasetImportRefJpaRepository refRepository) {
        this.productApplicationService = productApplicationService;
        this.partnerApplicationService = partnerApplicationService;
        this.purchaseApplicationService = purchaseApplicationService;
        this.salesApplicationService = salesApplicationService;
        this.customerInvoiceApplicationService = customerInvoiceApplicationService;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.stockPickingApplicationService = stockPickingApplicationService;
        this.posApplicationService = posApplicationService;
        this.accountJpaRepository = accountJpaRepository;
        this.journalJpaRepository = journalJpaRepository;
        this.uomJpaRepository = uomJpaRepository;
        this.warehouseJpaRepository = warehouseJpaRepository;
        this.refRepository = refRepository;
    }

    public DatasetImportReport importGroceryClasspath(UUID companyId) throws IOException {
        return importStreams(companyId, classpath("categories.csv"), classpath("partners.csv"),
                classpath("products.csv"), classpath("purchases.csv"), classpath("purchase_lines.csv"),
                classpath("sales_orders.csv"), classpath("sale_lines.csv"),
                classpath("pos_sessions.csv"), classpath("pos_tickets.csv"), classpath("pos_lines.csv"));
    }

    public DatasetImportReport importMultipart(UUID companyId,
                                               MultipartFile categories,
                                               MultipartFile partners,
                                               MultipartFile products,
                                               MultipartFile purchases,
                                               MultipartFile purchaseLines,
                                               MultipartFile salesOrders,
                                               MultipartFile saleLines,
                                               MultipartFile posSessions,
                                               MultipartFile posTickets,
                                               MultipartFile posLines) throws IOException {
        return importStreams(companyId,
                stream(categories), stream(partners), stream(products),
                stream(purchases), stream(purchaseLines),
                stream(salesOrders), stream(saleLines),
                stream(posSessions), stream(posTickets), stream(posLines));
    }

    private DatasetImportReport importStreams(UUID companyId,
                                              InputStream categories,
                                              InputStream partners,
                                              InputStream products,
                                              InputStream purchases,
                                              InputStream purchaseLines,
                                              InputStream salesOrders,
                                              InputStream saleLines,
                                              InputStream posSessions,
                                              InputStream posTickets,
                                              InputStream posLines) throws IOException {
        DatasetImportReport report = new DatasetImportReport();
        CompanyId company = new CompanyId(companyId);

        CsvTable categoryTable = CsvTable.parse(categories);
        CsvTable partnerTable = CsvTable.parse(partners);
        CsvTable productTable = CsvTable.parse(products);
        CsvTable purchaseTable = CsvTable.parse(purchases);
        CsvTable purchaseLineTable = CsvTable.parse(purchaseLines);
        CsvTable salesTable = CsvTable.parse(salesOrders);
        CsvTable saleLineTable = CsvTable.parse(saleLines);
        CsvTable sessionTable = CsvTable.parse(posSessions);
        CsvTable ticketTable = CsvTable.parse(posTickets);
        CsvTable posLineTable = CsvTable.parse(posLines);

        importCategories(company, categoryTable, report);
        ensureInventoryAccountsOnCategories(company);
        importPartners(company, partnerTable, report);
        importProducts(company, productTable, report);
        ensureVat10(companyId, report);

        if (!purchaseTable.isEmpty() && purchaseLineTable.isEmpty()) {
            report.warn("Purchases file has headers but no purchase_lines.csv — POs were not created. "
                    + "Stock, bills, and payments need line items.");
        } else {
            importPurchases(company, purchaseTable, purchaseLineTable, report);
        }

        if (!salesTable.isEmpty() && saleLineTable.isEmpty()) {
            report.warn("Sales orders file has headers but no sale_lines.csv — sales were not created.");
        } else {
            importSales(company, salesTable, saleLineTable, report);
        }

        if (!sessionTable.isEmpty() && posLineTable.isEmpty()) {
            report.warn("POS sessions/tickets are present but pos_lines.csv is missing — POS tickets were not created.");
        } else {
            importPos(company, sessionTable, ticketTable, posLineTable, report);
        }

        return report;
    }

    private void importCategories(CompanyId company, CsvTable table, DatasetImportReport report) {
        InventoryAccounts accounts = inventoryAccounts(company);
        UUID allId = findCategoryByName(company, "All").map(ProductCategoryResponse::getId).orElse(null);

        List<Map<String, String>> pending = new ArrayList<>(table.rows());
        int guard = 0;
        while (!pending.isEmpty() && guard++ < 20) {
            List<Map<String, String>> leftover = new ArrayList<>();
            for (Map<String, String> row : pending) {
                String code = CsvTable.get(row, "category_code", "code");
                String name = CsvTable.get(row, "name");
                String parentCode = CsvTable.get(row, "parent_code");
                if (code.isBlank() || name.isBlank()) {
                    report.error("Category row missing code/name");
                    continue;
                }
                Optional<ProductCategoryResponse> existing = findCategory(company, code, name);
                if (existing.isPresent()) {
                    ProductCategoryResponse cat = existing.get();
                    remember(company.getId(), CATEGORY, code, cat.getId());
                    patchCategoryAccounts(cat, accounts);
                    report.skipped("category", "Category already exists: " + code);
                    continue;
                }
                UUID parentId = null;
                if (!parentCode.isBlank()) {
                    Optional<UUID> parent = resolve(company.getId(), CATEGORY, parentCode);
                    if (parent.isEmpty()) {
                        leftover.add(row);
                        continue;
                    }
                    parentId = parent.get();
                } else if (allId != null) {
                    parentId = allId;
                }
                ProductCategoryCommand cmd = new ProductCategoryCommand();
                cmd.setCompanyId(company.getId());
                cmd.setName(name);
                cmd.setParentId(parentId);
                cmd.setValuationMethod(ValuationMethod.AVCO);
                applyAccounts(cmd, accounts);
                try {
                    ProductCategoryResponse created = productApplicationService.createCategory(cmd);
                    remember(company.getId(), CATEGORY, code, created.getId());
                    report.created("category");
                } catch (RuntimeException ex) {
                    report.error("Category " + code + ": " + ex.getMessage());
                }
            }
            if (leftover.size() == pending.size()) {
                leftover.forEach(r -> report.error("Category parent not found: " + CsvTable.get(r, "category_code")));
                break;
            }
            pending = leftover;
        }
    }

    private void importPartners(CompanyId company, CsvTable table, DatasetImportReport report) {
        for (Map<String, String> raw : table.rows()) {
            Map<String, String> row = alignPartnerCurrency(raw);
            String code = CsvTable.get(row, "partner_code", "vendor_code", "customer_code", "code");
            String name = CsvTable.get(row, "display_name");
            if (code.isBlank() || name.isBlank()) {
                report.error("Partner row missing code/display_name");
                continue;
            }
            Optional<UUID> existing = resolve(company.getId(), PARTNER, code);
            if (existing.isPresent() || findPartnerByName(company, name).isPresent()) {
                UUID id = existing.orElseGet(() -> findPartnerByName(company, name).map(PartnerResponse::getId).orElseThrow());
                remember(company.getId(), PARTNER, code, id);
                report.skipped("partner", "Partner already exists: " + code);
                continue;
            }
            CreatePartnerCommand cmd = new CreatePartnerCommand();
            cmd.setCompanyId(company.getId());
            cmd.setKind(parseKind(CsvTable.get(row, "kind")));
            cmd.setDisplayName(name);
            cmd.setLegalName(blankToNull(CsvTable.get(row, "legal_name")));
            cmd.setPhone(blankToNull(CsvTable.get(row, "phone")));
            cmd.setTaxId(blankToNull(CsvTable.get(row, "tax_id")));
            cmd.setEmail(blankToNull(CsvTable.get(row, "email")));
            cmd.setCurrencyCode(defaultIq(CsvTable.get(row, "currency")));
            cmd.setVendor(parseBool(CsvTable.get(row, "is_vendor")));
            cmd.setCustomer(parseBool(CsvTable.get(row, "is_customer")));
            cmd.setCreditLimit(parseDecimal(CsvTable.get(row, "credit_limit"), BigDecimal.ZERO));
            String payable = CsvTable.get(row, "payable_account_code");
            String receivable = CsvTable.get(row, "receivable_account_code");
            if (!payable.isBlank()) {
                cmd.setPayableAccountId(accountId(company.getId(), payable));
            }
            if (!receivable.isBlank()) {
                cmd.setReceivableAccountId(accountId(company.getId(), receivable));
            }
            try {
                PartnerResponse created = partnerApplicationService.createPartner(cmd);
                remember(company.getId(), PARTNER, code, created.getId());
                report.created("partner");
            } catch (RuntimeException ex) {
                report.error("Partner " + code + ": " + ex.getMessage());
            }
        }
    }

    private void importProducts(CompanyId company, CsvTable table, DatasetImportReport report) {
        Map<String, UUID> uoms = new LinkedHashMap<>();
        for (UomEntity u : uomJpaRepository.findByCompany(company.getId(), true)) {
            uoms.put(u.getName().toLowerCase(Locale.ROOT), u.getId());
        }
        for (Map<String, String> row : table.rows()) {
            String sku = CsvTable.get(row, "sku");
            if (sku.isBlank()) {
                report.error("Product row missing sku");
                continue;
            }
            Optional<ProductResponse> existing = findProductBySku(company, sku);
            if (existing.isPresent()) {
                remember(company.getId(), PRODUCT, sku, existing.get().getId());
                report.skipped("product", "Product already exists: " + sku);
                continue;
            }
            String categoryCode = CsvTable.get(row, "category_code");
            UUID categoryId = findCategory(company, categoryCode, categoryCode)
                    .map(ProductCategoryResponse::getId)
                    .orElse(null);
            if (categoryId == null) {
                report.error("Product " + sku + ": unknown category " + categoryCode);
                continue;
            }
            String uomName = CsvTable.get(row, "uom");
            UUID uomId = uoms.get(uomName.toLowerCase(Locale.ROOT));
            if (uomId == null) {
                report.error("Product " + sku + ": unknown uom " + uomName + " (use Unit or kg)");
                continue;
            }
            CreateProductCommand cmd = new CreateProductCommand();
            cmd.setCompanyId(company.getId());
            cmd.setSku(sku);
            cmd.setName(CsvTable.get(row, "name"));
            cmd.setBarcode(blankToNull(CsvTable.get(row, "barcode")));
            cmd.setDescription(blankToNull(CsvTable.get(row, "description")));
            cmd.setProductType(ProductType.valueOf(CsvTable.get(row, "type").toUpperCase(Locale.ROOT)));
            cmd.setCategoryId(categoryId);
            cmd.setUomId(uomId);
            cmd.setStandardCost(parseDecimal(CsvTable.get(row, "cost"), BigDecimal.ZERO));
            cmd.setListPrice(parseDecimal(CsvTable.get(row, "list_price"), BigDecimal.ZERO));
            cmd.setPurchaseOk(parseBool(CsvTable.get(row, "purchase_ok"), true));
            cmd.setSaleOk(parseBool(CsvTable.get(row, "sale_ok"), true));
            try {
                ProductResponse created = productApplicationService.createProduct(cmd);
                remember(company.getId(), PRODUCT, sku, created.getId());
                report.created("product");
            } catch (RuntimeException ex) {
                report.error("Product " + sku + ": " + ex.getMessage());
            }
        }
    }

    private void importPurchases(CompanyId company, CsvTable headers, CsvTable lines, DatasetImportReport report) {
        Map<String, List<Map<String, String>>> byPo = group(lines, "po_number");
        UUID warehouseId = warehouse(company.getId(), "WH").getId();
        UUID vat = resolve(company.getId(), TAX, "VAT10").orElse(null);
        for (Map<String, String> row : headers.rows()) {
            String poNumber = CsvTable.get(row, "po_number");
            List<Map<String, String>> poLines = byPo.getOrDefault(poNumber, List.of());
            if (poLines.isEmpty()) {
                report.error("PO " + poNumber + " has no lines");
                continue;
            }
            if (resolve(company.getId(), PO, poNumber).isPresent()) {
                report.skipped("purchase", "PO already exists: " + poNumber);
                continue;
            }
            String vendorCode = CsvTable.get(row, "vendor_code");
            UUID vendorId = resolve(company.getId(), PARTNER, vendorCode).orElse(null);
            if (vendorId == null) {
                report.error("PO " + poNumber + ": unknown vendor " + vendorCode);
                continue;
            }
            List<PurchaseOrderLineCommand> cmds = new ArrayList<>();
            boolean lineOk = true;
            for (Map<String, String> line : poLines) {
                String sku = CsvTable.get(line, "sku");
                ProductResponse product = product(company, sku);
                if (product == null) {
                    report.error("PO " + poNumber + ": unknown sku " + sku);
                    lineOk = false;
                    break;
                }
                PurchaseOrderLineCommand lc = new PurchaseOrderLineCommand();
                lc.setProductId(product.getId());
                lc.setName(product.getName());
                lc.setUomId(uomForLine(company.getId(), CsvTable.get(line, "uom"), product.getUomId()));
                lc.setWarehouseId(warehouseId);
                lc.setQtyOrdered(parseDecimal(CsvTable.get(line, "qty"), BigDecimal.ONE));
                lc.setUnitPrice(parseDecimal(CsvTable.get(line, "unit_price"), product.getStandardCost()));
                lc.setDiscountPercent(parseDecimal(CsvTable.get(line, "discount_percent"), BigDecimal.ZERO));
                String taxCode = CsvTable.get(line, "tax_code");
                if (!taxCode.isBlank() && vat != null) {
                    lc.setTaxIds(List.of(vat));
                }
                cmds.add(lc);
            }
            if (!lineOk) {
                continue;
            }
            CreatePurchaseOrderCommand cmd = new CreatePurchaseOrderCommand();
            cmd.setCompanyId(company.getId());
            cmd.setVendorPartnerId(vendorId);
            cmd.setName(poNumber);
            cmd.setCurrencyCode(defaultIq(CsvTable.get(row, "currency")));
            cmd.setWarehouseId(warehouseId);
            cmd.setOrderDate(parseDate(CsvTable.get(row, "order_date")));
            cmd.setNotes(blankToNull(CsvTable.get(row, "notes")));
            cmd.setLines(cmds);
            try {
                PurchaseOrderResponse po = purchaseApplicationService.createPurchaseOrder(cmd);
                po = purchaseApplicationService.confirmPurchaseOrder(po.getId());
                if (parseBool(CsvTable.get(row, "receive"), true)
                        && po.getReceiptPickingIds() != null && !po.getReceiptPickingIds().isEmpty()) {
                    purchaseApplicationService.validateReceiptPicking(po.getReceiptPickingIds().get(0), new ValidatePickingCommand());
                }
                if (parseBool(CsvTable.get(row, "create_bill"), true)) {
                    CreateVendorBillFromPoCommand billCmd = new CreateVendorBillFromPoCommand();
                    billCmd.setCompanyId(company.getId());
                    billCmd.setPurchaseOrderId(po.getId());
                    billCmd.setBillDate(po.getOrderDate());
                    billCmd.setDueDate(po.getOrderDate() != null ? po.getOrderDate().plusDays(7) : LocalDate.now());
                    billCmd.setReference(poNumber);
                    VendorBillResponse bill = purchaseApplicationService.createVendorBillFromPo(billCmd);
                    bill = purchaseApplicationService.postVendorBill(bill.getId());
                    if (parseBool(CsvTable.get(row, "pay_bill"), false)) {
                        UUID ap = accountId(company.getId(), "430004");
                        BigDecimal amount = sumCredit(bill.getJournalEntryId(), ap);
                        RegisterVendorPaymentCommand pay = new RegisterVendorPaymentCommand();
                        pay.setCompanyId(company.getId());
                        pay.setVendorBillId(bill.getId());
                        pay.setBankJournalId(journal(company.getId(), "430002").getId());
                        String payDate = CsvTable.get(row, "payment_date");
                        pay.setPaymentDate(payDate.isBlank() ? po.getOrderDate() : parseDate(payDate));
                        pay.setAmount(amount);
                        pay.setCurrencyCode(cmd.getCurrencyCode());
                        pay.setReference(poNumber);
                        purchaseApplicationService.registerVendorPayment(pay);
                    }
                }
                remember(company.getId(), PO, poNumber, po.getId());
                report.created("purchase");
            } catch (RuntimeException ex) {
                report.error("PO " + poNumber + ": " + ex.getMessage());
            }
        }
    }

    private void importSales(CompanyId company, CsvTable headers, CsvTable lines, DatasetImportReport report) {
        Map<String, List<Map<String, String>>> bySo = group(lines, "so_number");
        UUID warehouseId = warehouse(company.getId(), "WH").getId();
        UUID vat = resolve(company.getId(), TAX, "VAT10").orElse(null);
        for (Map<String, String> row : headers.rows()) {
            String soNumber = CsvTable.get(row, "so_number");
            List<Map<String, String>> soLines = bySo.getOrDefault(soNumber, List.of());
            if (soLines.isEmpty()) {
                report.error("SO " + soNumber + " has no lines");
                continue;
            }
            if (resolve(company.getId(), SO, soNumber).isPresent()) {
                report.skipped("sale", "SO already exists: " + soNumber);
                continue;
            }
            UUID customerId = resolve(company.getId(), PARTNER, CsvTable.get(row, "customer_code")).orElse(null);
            if (customerId == null) {
                report.error("SO " + soNumber + ": unknown customer " + CsvTable.get(row, "customer_code"));
                continue;
            }
            List<SalesOrderLineCommand> cmds = new ArrayList<>();
            boolean ok = true;
            for (Map<String, String> line : soLines) {
                ProductResponse product = product(company, CsvTable.get(line, "sku"));
                if (product == null) {
                    report.error("SO " + soNumber + ": unknown sku " + CsvTable.get(line, "sku"));
                    ok = false;
                    break;
                }
                SalesOrderLineCommand lc = new SalesOrderLineCommand();
                lc.setProductId(product.getId());
                lc.setName(product.getName());
                lc.setUomId(uomForLine(company.getId(), CsvTable.get(line, "uom"), product.getUomId()));
                lc.setQtyOrdered(parseDecimal(CsvTable.get(line, "qty"), BigDecimal.ONE));
                lc.setUnitPrice(parseDecimal(CsvTable.get(line, "unit_price"), product.getListPrice()));
                lc.setDiscountPercent(parseDecimal(CsvTable.get(line, "discount_percent"), BigDecimal.ZERO));
                if (!CsvTable.get(line, "tax_code").isBlank() && vat != null) {
                    lc.setTaxIds(List.of(vat));
                }
                cmds.add(lc);
            }
            if (!ok) {
                continue;
            }
            CreateSalesOrderCommand cmd = new CreateSalesOrderCommand();
            cmd.setCompanyId(company.getId());
            cmd.setCustomerPartnerId(customerId);
            cmd.setName(soNumber);
            cmd.setCurrencyCode(defaultIq(CsvTable.get(row, "currency")));
            cmd.setWarehouseId(warehouseId);
            cmd.setOrderDate(parseDate(CsvTable.get(row, "order_date")));
            cmd.setNotes(blankToNull(CsvTable.get(row, "notes")));
            cmd.setLines(cmds);
            try {
                SalesOrderResponse so = salesApplicationService.createSalesOrder(cmd);
                if (parseBool(CsvTable.get(row, "confirm"), true)) {
                    so = salesApplicationService.confirmSalesOrder(so.getId());
                }
                if (parseBool(CsvTable.get(row, "deliver"), true)
                        && so.getDeliveryPickingIds() != null) {
                    for (UUID pickingId : so.getDeliveryPickingIds()) {
                        stockPickingApplicationService.validatePicking(pickingId, new ValidatePickingCommand());
                    }
                    so = salesApplicationService.getSalesOrder(so.getId());
                }
                if (parseBool(CsvTable.get(row, "invoice"), true)) {
                    CreateCustomerInvoiceFromSalesOrderCommand invCmd = new CreateCustomerInvoiceFromSalesOrderCommand();
                    invCmd.setCompanyId(company.getId());
                    invCmd.setSalesOrderId(so.getId());
                    invCmd.setInvoiceDate(so.getOrderDate());
                    invCmd.setDueDate(so.getOrderDate() != null ? so.getOrderDate().plusDays(7) : LocalDate.now());
                    invCmd.setReference(soNumber);
                    CustomerInvoiceResponse inv = salesApplicationService.createCustomerInvoiceFromSalesOrder(invCmd);
                    inv = customerInvoiceApplicationService.postCustomerInvoice(inv.getId());
                    if (parseBool(CsvTable.get(row, "pay"), false)) {
                        UUID ar = accountId(company.getId(), "430003");
                        BigDecimal amount = sumDebit(inv.getJournalEntryId(), ar);
                        RegisterCustomerPaymentCommand pay = new RegisterCustomerPaymentCommand();
                        pay.setCompanyId(company.getId());
                        pay.setCustomerInvoiceId(inv.getId());
                        pay.setPaymentJournalId(journal(company.getId(), "430001").getId());
                        String payDate = CsvTable.get(row, "payment_date");
                        pay.setPaymentDate(payDate.isBlank() ? so.getOrderDate() : parseDate(payDate));
                        pay.setAmount(amount);
                        pay.setCurrencyCode(cmd.getCurrencyCode());
                        pay.setReference(soNumber);
                        customerInvoiceApplicationService.registerCustomerPayment(pay);
                    }
                }
                remember(company.getId(), SO, soNumber, so.getId());
                report.created("sale");
            } catch (RuntimeException ex) {
                report.error("SO " + soNumber + ": " + ex.getMessage());
            }
        }
    }

    private void importPos(CompanyId company, CsvTable sessions, CsvTable tickets, CsvTable lines,
                           DatasetImportReport report) {
        if (sessions.isEmpty()) {
            return;
        }
        UUID warehouseId = warehouse(company.getId(), "WH").getId();
        UUID cashCustomer = resolve(company.getId(), PARTNER, "C-CASH")
                .orElseThrow(() -> new IllegalStateException("C-CASH customer is required for POS import"));
        UUID cashJournal = journal(company.getId(), "430001").getId();
        UUID bankJournal = journal(company.getId(), "430002").getId();
        UUID vat = resolve(company.getId(), TAX, "VAT10").orElse(null);
        UUID configId = resolve(company.getId(), POS_CONFIG, "GROCERY").orElse(null);
        if (configId == null) {
            PosConfigCommand cfg = new PosConfigCommand();
            cfg.setCompanyId(company.getId());
            cfg.setName("Grocery POS");
            cfg.setWarehouseId(warehouseId);
            cfg.setDefaultCustomerPartnerId(cashCustomer);
            cfg.setCashJournalId(cashJournal);
            cfg.setBankJournalId(bankJournal);
            cfg.setCurrencyCode("IQD");
            PosConfigResponse created = posApplicationService.createConfig(cfg);
            configId = created.getId();
            remember(company.getId(), POS_CONFIG, "GROCERY", configId);
        }

        Map<String, List<Map<String, String>>> ticketsBySession = group(tickets, "session_code");
        Map<String, List<Map<String, String>>> linesByTicket = group(lines, "ticket_code");

        for (Map<String, String> sessionRow : sessions.rows()) {
            String sessionCode = CsvTable.get(sessionRow, "session_code");
            if (resolve(company.getId(), POS_SESSION, sessionCode).isPresent()) {
                report.skipped("pos-session", "POS session already exists: " + sessionCode);
                continue;
            }
            OpenPosSessionCommand open = new OpenPosSessionCommand();
            open.setCompanyId(company.getId());
            open.setConfigId(configId);
            open.setOpeningCash(parseDecimal(CsvTable.get(sessionRow, "opening_cash"), BigDecimal.ZERO));
            PosSessionResponse session;
            try {
                session = posApplicationService.openSession(open);
            } catch (RuntimeException ex) {
                report.error("POS session " + sessionCode + " open: " + ex.getMessage());
                continue;
            }
            BigDecimal cashIn = open.getOpeningCash();
            List<Map<String, String>> dayTickets = ticketsBySession.getOrDefault(sessionCode, List.of());
            for (Map<String, String> ticket : dayTickets) {
                String ticketCode = CsvTable.get(ticket, "ticket_code");
                List<Map<String, String>> tLines = linesByTicket.getOrDefault(ticketCode, List.of());
                if (tLines.isEmpty()) {
                    report.error("POS ticket " + ticketCode + " has no lines");
                    continue;
                }
                CheckoutPosOrderCommand checkout = new CheckoutPosOrderCommand();
                checkout.setCompanyId(company.getId());
                checkout.setSessionId(session.getId());
                UUID customer = resolve(company.getId(), PARTNER, CsvTable.get(ticket, "customer_code")).orElse(cashCustomer);
                checkout.setCustomerPartnerId(customer);
                checkout.setNote(blankToNull(CsvTable.get(ticket, "notes")));
                List<PosOrderLineCommand> orderLines = new ArrayList<>();
                boolean ok = true;
                for (Map<String, String> line : tLines) {
                    ProductResponse product = product(company, CsvTable.get(line, "sku"));
                    if (product == null) {
                        report.error("POS ticket " + ticketCode + ": unknown sku " + CsvTable.get(line, "sku"));
                        ok = false;
                        break;
                    }
                    PosOrderLineCommand lc = new PosOrderLineCommand();
                    lc.setProductId(product.getId());
                    lc.setName(product.getName());
                    lc.setUomId(uomForLine(company.getId(), CsvTable.get(line, "uom"), product.getUomId()));
                    lc.setQuantity(parseDecimal(CsvTable.get(line, "qty"), BigDecimal.ONE));
                    lc.setUnitPrice(parseDecimal(CsvTable.get(line, "unit_price"), product.getListPrice()));
                    lc.setDiscountPercent(parseDecimal(CsvTable.get(line, "discount_percent"), BigDecimal.ZERO));
                    if (!CsvTable.get(line, "tax_code").isBlank() && vat != null) {
                        lc.setTaxIds(List.of(vat));
                    }
                    orderLines.add(lc);
                }
                if (!ok) {
                    continue;
                }
                checkout.setLines(orderLines);
                RegisterPosPaymentCommand pay = new RegisterPosPaymentCommand();
                String method = CsvTable.get(ticket, "payment_method").toUpperCase(Locale.ROOT);
                pay.setMethod("CARD".equals(method) ? PosPaymentMethod.CARD : PosPaymentMethod.CASH);
                pay.setAmount(parseDecimal(CsvTable.get(ticket, "payment_amount"), BigDecimal.ONE));
                pay.setReference(ticketCode);
                if (pay.getMethod() == PosPaymentMethod.CARD) {
                    pay.setJournalId(bankJournal);
                }
                checkout.setPayments(List.of(pay));
                try {
                    posApplicationService.checkout(checkout);
                    if (pay.getMethod() == PosPaymentMethod.CASH) {
                        cashIn = cashIn.add(pay.getAmount());
                    }
                    report.created("pos-ticket");
                } catch (RuntimeException ex) {
                    report.error("POS ticket " + ticketCode + ": " + ex.getMessage());
                }
            }
            ClosePosSessionCommand close = new ClosePosSessionCommand();
            close.setClosingCash(cashIn);
            try {
                posApplicationService.closeSession(session.getId(), close);
                remember(company.getId(), POS_SESSION, sessionCode, session.getId());
                report.created("pos-session");
            } catch (RuntimeException ex) {
                report.error("POS session " + sessionCode + " close: " + ex.getMessage());
            }
        }
    }

    private void ensureVat10(UUID companyId, DatasetImportReport report) {
        Optional<UUID> existing = resolve(companyId, TAX, "VAT10");
        if (existing.isPresent()) {
            return;
        }
        for (FiscalTaxResponse t : purchaseApplicationService.listFiscalTaxes(companyId)) {
            if ("VAT 10%".equalsIgnoreCase(t.getName()) || "VAT10".equalsIgnoreCase(t.getName())) {
                remember(companyId, TAX, "VAT10", t.getId());
                return;
            }
        }
        CreateFiscalTaxCommand cmd = new CreateFiscalTaxCommand();
        cmd.setCompanyId(companyId);
        cmd.setName("VAT 10%");
        cmd.setAmountType(TaxAmountType.PERCENT);
        cmd.setAmount(new BigDecimal("10"));
        cmd.setPriceInclude(false);
        cmd.setScope(FiscalTaxScope.BOTH);
        cmd.setAccountId(accountId(companyId, "430013"));
        FiscalTaxResponse created = purchaseApplicationService.createFiscalTax(cmd);
        remember(companyId, TAX, "VAT10", created.getId());
        report.created("tax");
    }

    private Optional<UUID> resolve(UUID companyId, String type, String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return refRepository.findByCompanyIdAndRefTypeAndCode(companyId, type, code)
                .map(DatasetImportRefEntity::getEntityId);
    }

    private void remember(UUID companyId, String type, String code, UUID id) {
        DatasetImportRefEntity row = refRepository.findByCompanyIdAndRefTypeAndCode(companyId, type, code)
                .orElseGet(() -> new DatasetImportRefEntity(companyId, type, code, id));
        row.setEntityId(id);
        refRepository.save(row);
    }

    private Optional<ProductCategoryResponse> findCategoryByName(CompanyId company, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return productApplicationService.listCategories(company, false).stream()
                .filter(c -> name.equalsIgnoreCase(c.getName()))
                .findFirst();
    }

    private Optional<ProductCategoryResponse> findCategory(CompanyId company, String code, String name) {
        Optional<UUID> remembered = resolve(company.getId(), CATEGORY, code);
        if (remembered.isPresent()) {
            UUID id = remembered.get();
            return productApplicationService.listCategories(company, false).stream()
                    .filter(c -> id.equals(c.getId()))
                    .findFirst();
        }
        Optional<ProductCategoryResponse> byName = findCategoryByName(company, name);
        if (byName.isPresent()) {
            return byName;
        }
        String needle = (code == null || code.isBlank() ? name : code).trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        if (needle.isBlank()) {
            return Optional.empty();
        }
        return productApplicationService.listCategories(company, false).stream()
                .filter(c -> {
                    String compact = c.getName().replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
                    String firstWord = c.getName().split("[\\s&/\\-]")[0].toLowerCase(Locale.ROOT);
                    return compact.equals(needle)
                            || compact.startsWith(needle)
                            || compact.contains(needle)
                            || needle.startsWith(firstWord)
                            || needle.equals(firstWord)
                            || ("cig".equals(needle) && compact.contains("tobacco"));
                })
                .findFirst();
    }

    private record InventoryAccounts(UUID valuation, UUID input, UUID output, UUID cogs) {
        boolean ready() {
            return valuation != null && input != null && output != null && cogs != null;
        }
    }

    private InventoryAccounts inventoryAccounts(CompanyId company) {
        ProductCategoryResponse all = findCategoryByName(company, "All").orElse(null);
        UUID valuation = all != null ? all.getStockValuationAccountId() : null;
        UUID input = all != null ? all.getStockInputAccountId() : null;
        UUID output = all != null ? all.getStockOutputAccountId() : null;
        UUID cogs = all != null ? all.getCogsAccountId() : null;
        if (valuation == null) {
            valuation = accountIdOrNull(company.getId(), "430010");
        }
        if (input == null) {
            input = accountIdOrNull(company.getId(), "430011");
        }
        if (output == null) {
            output = accountIdOrNull(company.getId(), "430012");
        }
        if (cogs == null) {
            cogs = accountIdOrNull(company.getId(), "430009");
        }
        return new InventoryAccounts(valuation, input, output, cogs);
    }

    private UUID accountIdOrNull(UUID companyId, String code) {
        return accountJpaRepository.findByCompanyIdAndCode(companyId, code)
                .map(AccountEntity::getId)
                .orElse(null);
    }

    private static void applyAccounts(ProductCategoryCommand cmd, InventoryAccounts accounts) {
        if (accounts == null || !accounts.ready()) {
            return;
        }
        cmd.setStockValuationAccountId(accounts.valuation());
        cmd.setStockInputAccountId(accounts.input());
        cmd.setStockOutputAccountId(accounts.output());
        cmd.setCogsAccountId(accounts.cogs());
    }

    private void patchCategoryAccounts(ProductCategoryResponse cat, InventoryAccounts accounts) {
        if (cat == null || accounts == null || !accounts.ready()) {
            return;
        }
        if (cat.getStockValuationAccountId() != null
                && cat.getStockInputAccountId() != null
                && cat.getStockOutputAccountId() != null
                && cat.getCogsAccountId() != null) {
            return;
        }
        ProductCategoryCommand cmd = new ProductCategoryCommand();
        cmd.setCompanyId(cat.getCompanyId());
        cmd.setName(cat.getName());
        cmd.setParentId(cat.getParentId());
        cmd.setValuationMethod(cat.getValuationMethod());
        cmd.setStockValuationAccountId(
                cat.getStockValuationAccountId() != null ? cat.getStockValuationAccountId() : accounts.valuation());
        cmd.setStockInputAccountId(
                cat.getStockInputAccountId() != null ? cat.getStockInputAccountId() : accounts.input());
        cmd.setStockOutputAccountId(
                cat.getStockOutputAccountId() != null ? cat.getStockOutputAccountId() : accounts.output());
        cmd.setCogsAccountId(cat.getCogsAccountId() != null ? cat.getCogsAccountId() : accounts.cogs());
        productApplicationService.updateCategory(cat.getId(), cmd);
    }

    private void ensureInventoryAccountsOnCategories(CompanyId company) {
        InventoryAccounts accounts = inventoryAccounts(company);
        for (ProductCategoryResponse cat : productApplicationService.listCategories(company, false)) {
            patchCategoryAccounts(cat, accounts);
        }
    }

    private Optional<PartnerResponse> findPartnerByName(CompanyId company, String name) {
        return partnerApplicationService.search(company, name, null, null, false, PageRequest.of(0, 20))
                .getContent().stream()
                .filter(p -> name.equalsIgnoreCase(p.getDisplayName()))
                .findFirst();
    }

    private Optional<ProductResponse> findProductBySku(CompanyId company, String sku) {
        return productApplicationService.searchProducts(company, sku, false, PageRequest.of(0, 200))
                .getContent().stream()
                .filter(p -> sku.equalsIgnoreCase(p.getSku()))
                .findFirst()
                .or(() -> productApplicationService.searchProducts(company, "", false, PageRequest.of(0, 500))
                        .getContent().stream()
                        .filter(p -> sku.equalsIgnoreCase(p.getSku()))
                        .findFirst());
    }

    private ProductResponse product(CompanyId company, String sku) {
        return resolve(company.getId(), PRODUCT, sku)
                .flatMap(id -> Optional.of(productApplicationService.getProduct(id)))
                .orElseGet(() -> findProductBySku(company, sku).orElse(null));
    }

    private UUID uomForLine(UUID companyId, String uomName, UUID fallback) {
        if (uomName == null || uomName.isBlank()) {
            return fallback;
        }
        return uomJpaRepository.findByCompany(companyId, true).stream()
                .filter(u -> uomName.equalsIgnoreCase(u.getName()))
                .map(UomEntity::getId)
                .findFirst()
                .orElse(fallback);
    }

    private WarehouseEntity warehouse(UUID companyId, String code) {
        return warehouseJpaRepository.findByCompany(companyId, true).stream()
                .filter(w -> code.equalsIgnoreCase(w.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Warehouse " + code + " not found"));
    }

    private UUID accountId(UUID companyId, String code) {
        return accountJpaRepository.findByCompanyIdAndCode(companyId, code)
                .map(AccountEntity::getId)
                .orElseThrow(() -> new IllegalStateException("Account " + code + " not found"));
    }

    private JournalEntity journal(UUID companyId, String code) {
        return journalJpaRepository.findByCompanyIdAndCode(companyId, code)
                .orElseThrow(() -> new IllegalStateException("Journal " + code + " not found"));
    }

    private BigDecimal sumCredit(UUID journalEntryId, UUID accountId) {
        JournalEntryResponse je = journalEntryApplicationService.getJournalEntry(journalEntryId);
        BigDecimal sum = BigDecimal.ZERO;
        for (JournalEntryResponse.JournalItemResponse it : je.getItems()) {
            if (accountId.equals(it.getAccountId()) && it.getCredit() != null) {
                sum = sum.add(it.getCredit());
            }
        }
        return sum;
    }

    private BigDecimal sumDebit(UUID journalEntryId, UUID accountId) {
        JournalEntryResponse je = journalEntryApplicationService.getJournalEntry(journalEntryId);
        BigDecimal sum = BigDecimal.ZERO;
        for (JournalEntryResponse.JournalItemResponse it : je.getItems()) {
            if (accountId.equals(it.getAccountId()) && it.getDebit() != null) {
                sum = sum.add(it.getDebit());
            }
        }
        return sum;
    }

    private static Map<String, List<Map<String, String>>> group(CsvTable table, String key) {
        Map<String, List<Map<String, String>>> map = new LinkedHashMap<>();
        for (Map<String, String> row : table.rows()) {
            map.computeIfAbsent(CsvTable.get(row, key), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    static Map<String, String> alignPartnerCurrency(Map<String, String> row) {
        String currency = CsvTable.get(row, "currency");
        String isVendor = CsvTable.get(row, "is_vendor");
        if (!currency.isBlank() || !looksCurrency(isVendor)) {
            return row;
        }
        Map<String, String> copy = new LinkedHashMap<>(row);
        copy.put("currency", isVendor);
        copy.put("is_vendor", CsvTable.get(row, "is_customer"));
        copy.put("is_customer", CsvTable.get(row, "payable_account_code"));
        copy.put("payable_account_code", CsvTable.get(row, "receivable_account_code"));
        copy.put("receivable_account_code", CsvTable.get(row, "credit_limit"));
        copy.put("credit_limit", "0");
        return copy;
    }

    private static boolean looksCurrency(String v) {
        return v != null && v.length() == 3 && v.chars().allMatch(Character::isLetter);
    }

    static PartnerKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return PartnerKind.COMPANY;
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if ("PERSON".equals(v) || "INDIVIDUAL".equals(v)) {
            return PartnerKind.INDIVIDUAL;
        }
        return PartnerKind.COMPANY;
    }

    private static boolean parseBool(String raw) {
        return parseBool(raw, false);
    }

    private static boolean parseBool(String raw, boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "y" -> true;
            case "false", "0", "no", "n" -> false;
            default -> defaultValue;
        };
    }

    private static BigDecimal parseDecimal(String raw, BigDecimal fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return new BigDecimal(raw.trim());
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDate.now();
        }
        String v = raw.trim();
        if (v.length() > 10) {
            return LocalDateTime.parse(v.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
        }
        return LocalDate.parse(v);
    }

    private static String defaultIq(String currency) {
        return currency == null || currency.isBlank() ? "IQD" : currency;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private static InputStream classpath(String name) throws IOException {
        ClassPathResource resource = new ClassPathResource("dataset/grocery/" + name);
        if (!resource.exists()) {
            return InputStream.nullInputStream();
        }
        return resource.getInputStream();
    }

    private static InputStream stream(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return InputStream.nullInputStream();
        }
        return file.getInputStream();
    }
}
