package com.jalaldeveloper.accountingsystem.bootstrap;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.RegisterCustomerPaymentCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CustomerInvoiceApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerKind;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.CreatePartnerCommand;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PartnerResponse;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PartnerApplicationService;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccountEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccountJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.ProductCategoryEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.UomEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.WarehouseEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.ProductCategoryJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.UomJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.WarehouseJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateProductCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.ProductApplicationService;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.FiscalTaxScope;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.TaxAmountType;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.CreateFiscalTaxCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.CreatePurchaseOrderCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.CreateVendorBillFromPoCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.FiscalTaxResponse;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.PurchaseOrderLineCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.RegisterVendorPaymentCommand;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.dto.VendorBillResponse;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalInvoicePolicy;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderLineCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.input.SalesApplicationService;
import com.jalaldeveloper.accountingsystem.web.DashboardController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Optional demo dataset: partners, products, a purchase (receipt → bill → payment),
 * and a sales order (service line → invoice → payment). Idempotent via marker partner name.
 */
@Component
@Order(30)
@ConditionalOnProperty(
        name = "accounting.seed.demo-business-data",
        havingValue = "true",
        matchIfMissing = false)
public class DemoBusinessDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoBusinessDataSeeder.class);

    private static final UUID COMPANY_ID = DashboardController.DEFAULT_COMPANY_ID;
    private static final String MARKER_CUSTOMER = "Demo Customer (Seed)";
    private static final String MARKER_VENDOR = "Demo Vendor (Seed)";
    private static final String DEMO_TAX_NAME = "Demo VAT 10%";
    private static final String SKU_WIDGET = "DEMO-WIDGET";
    private static final String SKU_SERVICE = "DEMO-SVC";

    private final TransactionTemplate transactionTemplate;
    private final PartnerApplicationService partnerApplicationService;
    private final ProductApplicationService productApplicationService;
    private final PurchaseApplicationService purchaseApplicationService;
    private final SalesApplicationService salesApplicationService;
    private final CustomerInvoiceApplicationService customerInvoiceApplicationService;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final AccountJpaRepository accountJpaRepository;
    private final JournalJpaRepository journalJpaRepository;
    private final ProductCategoryJpaRepository productCategoryJpaRepository;
    private final UomJpaRepository uomJpaRepository;
    private final WarehouseJpaRepository warehouseJpaRepository;

    public DemoBusinessDataSeeder(TransactionTemplate transactionTemplate,
                                  PartnerApplicationService partnerApplicationService,
                                  ProductApplicationService productApplicationService,
                                  PurchaseApplicationService purchaseApplicationService,
                                  SalesApplicationService salesApplicationService,
                                  CustomerInvoiceApplicationService customerInvoiceApplicationService,
                                  JournalEntryApplicationService journalEntryApplicationService,
                                  AccountJpaRepository accountJpaRepository,
                                  JournalJpaRepository journalJpaRepository,
                                  ProductCategoryJpaRepository productCategoryJpaRepository,
                                  UomJpaRepository uomJpaRepository,
                                  WarehouseJpaRepository warehouseJpaRepository) {
        this.transactionTemplate = transactionTemplate;
        this.partnerApplicationService = partnerApplicationService;
        this.productApplicationService = productApplicationService;
        this.purchaseApplicationService = purchaseApplicationService;
        this.salesApplicationService = salesApplicationService;
        this.customerInvoiceApplicationService = customerInvoiceApplicationService;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.accountJpaRepository = accountJpaRepository;
        this.journalJpaRepository = journalJpaRepository;
        this.productCategoryJpaRepository = productCategoryJpaRepository;
        this.uomJpaRepository = uomJpaRepository;
        this.warehouseJpaRepository = warehouseJpaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> seedInTransaction());
    }

    private void seedInTransaction() {
        CompanyId companyIdVo = new CompanyId(COMPANY_ID);
        if (partnerApplicationService.search(companyIdVo, MARKER_CUSTOMER, true, null, false, PageRequest.of(0, 5))
                .getContent().stream().anyMatch(p -> MARKER_CUSTOMER.equals(p.getDisplayName()))) {
            log.info("Demo business data already present ({}), skipping seed.", MARKER_CUSTOMER);
            return;
        }

        UUID arId = requireAccountCode("430003");
        UUID apId = requireAccountCode("430004");
        UUID vatAccountId = requireAccountCode("430013");
        JournalEntity bankJournal = requireJournalCode("430002");
        JournalEntity cashJournal = requireJournalCode("430001");

        ProductCategoryEntity category = productCategoryJpaRepository.findByCompany(COMPANY_ID, true).stream()
                .filter(c -> "All".equalsIgnoreCase(c.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Product category 'All' not found; run inventory seed first"));
        UomEntity unitUom = uomJpaRepository.findByCompany(COMPANY_ID, true).stream()
                .filter(u -> "Unit".equalsIgnoreCase(u.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("UoM 'Unit' not found; run inventory seed first"));
        WarehouseEntity warehouse = warehouseJpaRepository.findByCompany(COMPANY_ID, true).stream()
                .filter(w -> "WH".equalsIgnoreCase(w.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Warehouse WH not found; run inventory seed first"));

        UUID taxId = ensureDemoTax(vatAccountId);

        PartnerResponse customer = partnerApplicationService.createPartner(partnerCmd(
                MARKER_CUSTOMER, true, false, arId, null));
        PartnerResponse vendor = partnerApplicationService.createPartner(partnerCmd(
                MARKER_VENDOR, false, true, null, apId));

        ProductResponse widget = productApplicationService.createProduct(
                stockProductCommand(SKU_WIDGET, "Demo Widget", category.getId(), unitUom.getId()));
        ProductResponse service = productApplicationService.createProduct(
                serviceProductCommand(SKU_SERVICE, "Demo Consulting", category.getId(), unitUom.getId()));

        LocalDate docDate = LocalDate.of(2026, 1, 15);

        /* --- Purchase: PO → receipt → vendor bill → post → bank payment --- */
        PurchaseOrderLineCommand pol = new PurchaseOrderLineCommand();
        pol.setProductId(widget.getId());
        pol.setName("Demo purchase line");
        pol.setUomId(unitUom.getId());
        pol.setWarehouseId(warehouse.getId());
        pol.setQtyOrdered(new BigDecimal("10"));
        pol.setUnitPrice(new BigDecimal("25"));
        pol.setDiscountPercent(BigDecimal.ZERO);
        pol.setTaxIds(List.of(taxId));

        CreatePurchaseOrderCommand poCmd = new CreatePurchaseOrderCommand();
        poCmd.setCompanyId(COMPANY_ID);
        poCmd.setVendorPartnerId(vendor.getId());
        poCmd.setName("PO-DEMO-SEED");
        poCmd.setCurrencyCode("USD");
        poCmd.setWarehouseId(warehouse.getId());
        poCmd.setOrderDate(docDate);
        poCmd.setLines(List.of(pol));

        var po = purchaseApplicationService.createPurchaseOrder(poCmd);
        po = purchaseApplicationService.confirmPurchaseOrder(po.getId());
        UUID pickingId = po.getReceiptPickingIds().get(0);
        purchaseApplicationService.validateReceiptPicking(pickingId, null);

        CreateVendorBillFromPoCommand billCmd = new CreateVendorBillFromPoCommand();
        billCmd.setCompanyId(COMPANY_ID);
        billCmd.setPurchaseOrderId(po.getId());
        billCmd.setBillDate(docDate);
        billCmd.setDueDate(docDate.plusDays(30));
        billCmd.setReference("BILL-DEMO-SEED");
        VendorBillResponse bill = purchaseApplicationService.createVendorBillFromPo(billCmd);
        bill = purchaseApplicationService.postVendorBill(bill.getId());

        BigDecimal apPay = sumCreditOnAccount(bill.getJournalEntryId(), apId);
        RegisterVendorPaymentCommand vp = new RegisterVendorPaymentCommand();
        vp.setCompanyId(COMPANY_ID);
        vp.setVendorBillId(bill.getId());
        vp.setBankJournalId(bankJournal.getId());
        vp.setPaymentDate(docDate);
        vp.setAmount(apPay);
        vp.setCurrencyCode("USD");
        vp.setReference("Demo vendor payment");
        purchaseApplicationService.registerVendorPayment(vp);

        /* --- Sales: service order (invoice on order) → invoice → cash payment --- */
        SalesOrderLineCommand sol = new SalesOrderLineCommand();
        sol.setProductId(service.getId());
        sol.setName("Implementation support");
        sol.setUomId(unitUom.getId());
        sol.setQtyOrdered(new BigDecimal("4"));
        sol.setUnitPrice(new BigDecimal("150"));
        sol.setDiscountPercent(BigDecimal.ZERO);
        sol.setTaxIds(List.of(taxId));
        sol.setInvoicePolicy(SalInvoicePolicy.ORDERED);

        CreateSalesOrderCommand soCmd = new CreateSalesOrderCommand();
        soCmd.setCompanyId(COMPANY_ID);
        soCmd.setCustomerPartnerId(customer.getId());
        soCmd.setName("SO-DEMO-SEED");
        soCmd.setCurrencyCode("USD");
        soCmd.setWarehouseId(warehouse.getId());
        soCmd.setOrderDate(docDate);
        soCmd.setLines(List.of(sol));

        SalesOrderResponse so = salesApplicationService.createSalesOrder(soCmd);
        so = salesApplicationService.confirmSalesOrder(so.getId());

        CreateCustomerInvoiceFromSalesOrderCommand invCmd = new CreateCustomerInvoiceFromSalesOrderCommand();
        invCmd.setCompanyId(COMPANY_ID);
        invCmd.setSalesOrderId(so.getId());
        invCmd.setInvoiceDate(docDate);
        invCmd.setDueDate(docDate.plusDays(14));
        invCmd.setReference("INV-DEMO-SEED");
        CustomerInvoiceResponse inv = salesApplicationService.createCustomerInvoiceFromSalesOrder(invCmd);
        inv = customerInvoiceApplicationService.postCustomerInvoice(inv.getId());

        BigDecimal arDue = sumDebitOnAccount(inv.getJournalEntryId(), arId);
        RegisterCustomerPaymentCommand cp = new RegisterCustomerPaymentCommand();
        cp.setCompanyId(COMPANY_ID);
        cp.setCustomerInvoiceId(inv.getId());
        cp.setPaymentJournalId(cashJournal.getId());
        cp.setPaymentDate(docDate);
        cp.setAmount(arDue);
        cp.setCurrencyCode("USD");
        cp.setReference("Demo customer payment");
        customerInvoiceApplicationService.registerCustomerPayment(cp);

        log.info("Seeded demo business data: customer {}, vendor {}, PO {}, SO {}, bill {}, invoice {}, payments posted.",
                customer.getId(), vendor.getId(), po.getId(), so.getId(), bill.getId(), inv.getId());
    }

    private static CreatePartnerCommand partnerCmd(String name, boolean customer, boolean vendor,
                                                     UUID receivableId, UUID payableId) {
        CreatePartnerCommand c = new CreatePartnerCommand();
        c.setCompanyId(COMPANY_ID);
        c.setKind(PartnerKind.COMPANY);
        c.setDisplayName(name);
        c.setCustomer(customer);
        c.setVendor(vendor);
        c.setCurrencyCode("USD");
        c.setReceivableAccountId(receivableId);
        c.setPayableAccountId(payableId);
        return c;
    }

    private CreateProductCommand stockProductCommand(String sku, String name, UUID categoryId, UUID uomId) {
        CreateProductCommand c = new CreateProductCommand();
        c.setCompanyId(COMPANY_ID);
        c.setSku(sku);
        c.setName(name);
        c.setProductType(ProductType.STOCKABLE);
        c.setCategoryId(categoryId);
        c.setUomId(uomId);
        c.setStandardCost(new BigDecimal("18"));
        c.setListPrice(new BigDecimal("40"));
        return c;
    }

    private CreateProductCommand serviceProductCommand(String sku, String name, UUID categoryId, UUID uomId) {
        CreateProductCommand c = new CreateProductCommand();
        c.setCompanyId(COMPANY_ID);
        c.setSku(sku);
        c.setName(name);
        c.setProductType(ProductType.SERVICE);
        c.setCategoryId(categoryId);
        c.setUomId(uomId);
        c.setListPrice(new BigDecimal("150"));
        return c;
    }

    private UUID ensureDemoTax(UUID vatAccountId) {
        for (FiscalTaxResponse t : purchaseApplicationService.listFiscalTaxes(COMPANY_ID)) {
            if (DEMO_TAX_NAME.equals(t.getName())) {
                return t.getId();
            }
        }
        CreateFiscalTaxCommand cmd = new CreateFiscalTaxCommand();
        cmd.setCompanyId(COMPANY_ID);
        cmd.setName(DEMO_TAX_NAME);
        cmd.setAmountType(TaxAmountType.PERCENT);
        cmd.setAmount(new BigDecimal("10"));
        cmd.setPriceInclude(false);
        cmd.setScope(FiscalTaxScope.BOTH);
        cmd.setAccountId(vatAccountId);
        return purchaseApplicationService.createFiscalTax(cmd).getId();
    }

    private UUID requireAccountCode(String code) {
        return accountJpaRepository.findByCompanyIdAndCode(COMPANY_ID, code)
                .map(AccountEntity::getId)
                .orElseThrow(() -> new IllegalStateException("Account " + code + " not found; run chart seed first"));
    }

    private JournalEntity requireJournalCode(String code) {
        return journalJpaRepository.findByCompanyIdAndCode(COMPANY_ID, code)
                .orElseThrow(() -> new IllegalStateException("Journal " + code + " not found; run chart seed first"));
    }

    private BigDecimal sumDebitOnAccount(UUID journalEntryId, UUID accountId) {
        if (journalEntryId == null) {
            throw new IllegalStateException("Expected posted journal entry");
        }
        JournalEntryResponse je = journalEntryApplicationService.getJournalEntry(journalEntryId);
        BigDecimal sum = BigDecimal.ZERO;
        for (JournalEntryResponse.JournalItemResponse it : je.getItems()) {
            if (accountId.equals(it.getAccountId())) {
                sum = sum.add(it.getDebit() != null ? it.getDebit() : BigDecimal.ZERO);
            }
        }
        if (sum.signum() <= 0) {
            throw new IllegalStateException("No debit on account " + accountId + " for entry " + journalEntryId);
        }
        return sum.setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal sumCreditOnAccount(UUID journalEntryId, UUID accountId) {
        if (journalEntryId == null) {
            throw new IllegalStateException("Expected posted journal entry");
        }
        JournalEntryResponse je = journalEntryApplicationService.getJournalEntry(journalEntryId);
        BigDecimal sum = BigDecimal.ZERO;
        for (JournalEntryResponse.JournalItemResponse it : je.getItems()) {
            if (accountId.equals(it.getAccountId())) {
                sum = sum.add(it.getCredit() != null ? it.getCredit() : BigDecimal.ZERO);
            }
        }
        if (sum.signum() <= 0) {
            throw new IllegalStateException("No credit on account " + accountId + " for entry " + journalEntryId);
        }
        return sum.setScale(4, java.math.RoundingMode.HALF_UP);
    }
}
