package com.jalaldeveloper.accountingsystem.platform.bootstrap;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.CompanyEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.PermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RolePermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.CompanyJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.PermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RoleJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RolePermissionJpaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Idempotently inserts the canonical permission catalog and a baseline role set
 * for the demo company. Mirrors the existing {@code DefaultCompanyChartDataSeeder}
 * pattern in the accounting service.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "platform.seed.rbac", havingValue = "true", matchIfMissing = true)
public class PlatformRbacSeeder implements ApplicationRunner {

    /** Default demo company id; matches {@code DashboardController.DEFAULT_COMPANY_ID}. */
    public static final UUID DEFAULT_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final List<String> PERMISSION_CATALOG = List.of(
            // Platform
            "platform.activity.read", "platform.activity.write",
            "platform.audit.read",
            "platform.user.read", "platform.user.write",
            "platform.role.read", "platform.role.write",
            "platform.permission.read",
            "platform.company.read", "platform.company.write",
            // Contacts
            "contacts.partner.read", "contacts.partner.write", "contacts.partner.archive",
            "contacts.payment-terms.read", "contacts.payment-terms.write",
            // Inventory (registered up-front so role assignment doesn't need to know about future modules)
            "inventory.product.read", "inventory.product.write",
            "inventory.product-category.read", "inventory.product-category.write",
            "inventory.uom.read", "inventory.uom.write",
            "inventory.warehouse.read", "inventory.warehouse.write",
            "inventory.picking.read", "inventory.picking.write",
            "inventory.picking.confirm", "inventory.picking.validate",
            "inventory.picking.cancel", "inventory.picking.return",
            "inventory.quant.read", "inventory.valuation.read",
            // Purchase
            "purchase.order.read", "purchase.order.write", "purchase.order.confirm",
            "purchase.receipt.validate",
            "purchase.vendor-bill.read", "purchase.vendor-bill.write", "purchase.vendor-bill.post",
            "purchase.payment.register",
            "purchase.fiscal-tax.read", "purchase.fiscal-tax.write",
            // Sales
            "sales.order.read", "sales.order.write", "sales.order.confirm",
            "sales.invoice.read", "sales.invoice.write",
            // POS
            "pos.config.read", "pos.config.write",
            "pos.session.open", "pos.session.close",
            "pos.order.read", "pos.order.write", "pos.order.pay", "pos.order.finalize",
            "pos.receipt.read",
            // Accounting (existing endpoints; not yet annotated)
            "accounting.account.read", "accounting.account.write",
            "accounting.journal.read", "accounting.journal.write",
            "accounting.journal-entry.read", "accounting.journal-entry.write",
            "accounting.journal-entry.post", "accounting.journal-entry.reverse",
            "accounting.fiscal-period.read", "accounting.fiscal-period.write",
            "accounting.currency.read", "accounting.currency.write",
            "accounting.report.read",
            "accounting.customer-invoice.read", "accounting.customer-invoice.write",
            "accounting.customer-invoice.post", "accounting.customer-payment.register",
            "accounting.vendor-bill.read", "accounting.vendor-bill.write", "accounting.vendor-bill.post",
            "accounting.vendor-payment.register"
    );

    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
            "ADMIN", Set.copyOf(PERMISSION_CATALOG),
            "ACCOUNTANT", Set.of(
                    "platform.activity.read", "platform.activity.write", "platform.audit.read",
                    "contacts.partner.read", "contacts.payment-terms.read",
                    "purchase.order.read", "purchase.vendor-bill.read", "purchase.vendor-bill.post",
                    "purchase.payment.register", "purchase.fiscal-tax.read",
                    "sales.order.read", "sales.invoice.read", "sales.invoice.write",
                    "accounting.account.read", "accounting.account.write",
                    "accounting.journal.read", "accounting.journal.write",
                    "accounting.journal-entry.read", "accounting.journal-entry.write",
                    "accounting.journal-entry.post", "accounting.journal-entry.reverse",
                    "accounting.fiscal-period.read", "accounting.fiscal-period.write",
                    "accounting.currency.read", "accounting.currency.write",
                    "accounting.report.read",
                    "accounting.customer-invoice.read", "accounting.customer-invoice.write",
                    "accounting.customer-invoice.post", "accounting.customer-payment.register",
                    "accounting.vendor-bill.read", "accounting.vendor-bill.write", "accounting.vendor-bill.post",
                    "accounting.vendor-payment.register"),
            "SALES", Set.of(
                    "platform.activity.read", "platform.activity.write",
                    "contacts.partner.read", "contacts.partner.write",
                    "inventory.product.read", "inventory.warehouse.read",
                    "inventory.picking.read", "inventory.picking.write",
                    "inventory.picking.confirm", "inventory.picking.validate",
                    "sales.order.read", "sales.order.write", "sales.order.confirm",
                    "sales.invoice.read", "sales.invoice.write",
                    "pos.config.read", "pos.config.write",
                    "pos.session.open", "pos.session.close",
                    "pos.order.read", "pos.order.write", "pos.order.pay", "pos.order.finalize",
                    "pos.receipt.read",
                    "accounting.customer-invoice.read", "accounting.customer-invoice.write",
                    "accounting.customer-invoice.post", "accounting.customer-payment.register"),
            "PURCHASING", Set.of(
                    "platform.activity.read", "platform.activity.write",
                    "contacts.partner.read", "contacts.partner.write",
                    "inventory.product.read", "inventory.warehouse.read",
                    "inventory.picking.read", "inventory.picking.write",
                    "inventory.picking.confirm", "inventory.picking.validate",
                    "purchase.order.read", "purchase.order.write", "purchase.order.confirm",
                    "purchase.receipt.validate",
                    "purchase.vendor-bill.read", "purchase.vendor-bill.write", "purchase.vendor-bill.post",
                    "purchase.payment.register",
                    "purchase.fiscal-tax.read", "purchase.fiscal-tax.write",
                    "accounting.vendor-bill.read", "accounting.vendor-bill.write", "accounting.vendor-bill.post",
                    "accounting.vendor-payment.register"),
            "WAREHOUSE", Set.of(
                    "platform.activity.read", "platform.activity.write",
                    "inventory.product.read",
                    "inventory.warehouse.read", "inventory.warehouse.write",
                    "inventory.picking.read", "inventory.picking.write",
                    "inventory.picking.confirm", "inventory.picking.validate",
                    "inventory.picking.cancel", "inventory.picking.return",
                    "inventory.quant.read", "inventory.valuation.read"),
            "READONLY", Set.of(
                    "platform.activity.read", "platform.audit.read",
                    "platform.company.read", "platform.permission.read",
                    "contacts.partner.read", "contacts.payment-terms.read",
                    "inventory.product.read", "inventory.warehouse.read",
                    "inventory.picking.read", "inventory.quant.read", "inventory.valuation.read",
                    "purchase.order.read", "purchase.vendor-bill.read", "purchase.fiscal-tax.read",
                    "sales.order.read", "sales.invoice.read",
                    "pos.config.read", "pos.order.read", "pos.receipt.read",
                    "accounting.account.read", "accounting.journal.read",
                    "accounting.journal-entry.read", "accounting.fiscal-period.read",
                    "accounting.currency.read",
                    "accounting.report.read",
                    "accounting.customer-invoice.read",
                    "accounting.vendor-bill.read")
    );

    private final PermissionJpaRepository permissionRepository;
    private final RoleJpaRepository roleRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final CompanyJpaRepository companyRepository;

    public PlatformRbacSeeder(PermissionJpaRepository permissionRepository,
                              RoleJpaRepository roleRepository,
                              RolePermissionJpaRepository rolePermissionRepository,
                              CompanyJpaRepository companyRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDefaultCompany();
        seedPermissions();
        seedRolesForCompany(DEFAULT_COMPANY_ID);
    }

    private void seedDefaultCompany() {
        if (companyRepository.existsById(DEFAULT_COMPANY_ID)) {
            return;
        }
        CompanyEntity c = new CompanyEntity();
        c.setId(DEFAULT_COMPANY_ID);
        c.setName("Demo Company");
        c.setLegalName("Demo Company LLC");
        c.setEmail("contact@demo.local");
        c.setCountry("US");
        c.setDefaultCurrency("USD");
        c.setLocale("en-US");
        c.setDateFormat("yyyy-MM-dd");
        c.setNumberFormat("#,##0.00");
        c.setFiscalYearStartMonth(1);
        companyRepository.save(c);
    }

    private void seedPermissions() {
        for (String code : PERMISSION_CATALOG) {
            if (!permissionRepository.existsByCode(code)) {
                PermissionEntity p = new PermissionEntity();
                p.setId(UUID.randomUUID());
                p.setCode(code);
                p.setDescription(code);
                permissionRepository.save(p);
            }
        }
    }

    private void seedRolesForCompany(UUID companyId) {
        ROLE_PERMISSIONS.forEach((roleCode, permissionCodes) -> {
            RoleEntity role = roleRepository.findByCompanyIdAndCode(companyId, roleCode).orElseGet(() -> {
                RoleEntity r = new RoleEntity();
                r.setId(UUID.randomUUID());
                r.setCompanyId(companyId);
                r.setCode(roleCode);
                r.setName(roleCode);
                r.setDescription("Auto-seeded role: " + roleCode);
                return roleRepository.save(r);
            });
            assignPermissions(role, permissionCodes);
        });
    }

    private void assignPermissions(RoleEntity role, Set<String> permissionCodes) {
        List<PermissionEntity> permissions = permissionRepository.findByCodeIn(List.copyOf(permissionCodes));
        for (PermissionEntity p : permissions) {
            RolePermissionEntity.PK pk = new RolePermissionEntity.PK(role.getId(), p.getId());
            if (!rolePermissionRepository.existsById(pk)) {
                rolePermissionRepository.save(new RolePermissionEntity(role.getId(), p.getId()));
            }
        }
    }
}
