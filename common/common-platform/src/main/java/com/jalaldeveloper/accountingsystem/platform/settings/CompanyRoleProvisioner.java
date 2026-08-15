package com.jalaldeveloper.accountingsystem.platform.settings;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.PermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RolePermissionEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.UserRoleEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.PermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RoleJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RolePermissionJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.UserRoleJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Copies the canonical system role set onto a company and optionally grants ADMIN
 * to the user who created it. Without this, {@code listForUser} never returns the
 * new tenant because membership is derived from {@code platform_user_role}.
 */
@Service
public class CompanyRoleProvisioner {

    static final List<String> PERMISSION_CATALOG = List.of(
            "platform.activity.read", "platform.activity.write",
            "platform.audit.read",
            "platform.user.read", "platform.user.write",
            "platform.role.read", "platform.role.write",
            "platform.permission.read",
            "platform.company.read", "platform.company.write",
            "dataset.import",
            "contacts.partner.read", "contacts.partner.write", "contacts.partner.archive",
            "contacts.payment-terms.read", "contacts.payment-terms.write",
            "hr.employee.read", "hr.employee.write", "hr.employee.archive",
            "hr.department.read", "hr.department.write",
            "hr.attendance.read", "hr.attendance.write",
            "hr.time-off.read", "hr.time-off.write", "hr.time-off.approve",
            "hr.employee.self.read", "hr.attendance.self.read",
            "hr.time-off.self.read", "hr.time-off.self.write",
            "payroll.read", "payroll.write", "payroll.post", "payroll.pay",
            "payroll.payslip.self.read",
            "expense.read", "expense.write", "expense.approve", "expense.post",
            "inventory.product.read", "inventory.product.write",
            "inventory.product-category.read", "inventory.product-category.write",
            "inventory.uom.read", "inventory.uom.write",
            "inventory.warehouse.read", "inventory.warehouse.write",
            "inventory.picking.read", "inventory.picking.write",
            "inventory.picking.confirm", "inventory.picking.validate",
            "inventory.picking.cancel", "inventory.picking.return",
            "inventory.quant.read", "inventory.valuation.read",
            "purchase.order.read", "purchase.order.write", "purchase.order.confirm",
            "purchase.receipt.validate",
            "purchase.vendor-bill.read", "purchase.vendor-bill.write", "purchase.vendor-bill.post",
            "purchase.payment.register",
            "purchase.fiscal-tax.read", "purchase.fiscal-tax.write",
            "sales.order.read", "sales.order.write", "sales.order.confirm",
            "sales.invoice.read", "sales.invoice.write",
            "pos.config.read", "pos.config.write",
            "pos.session.open", "pos.session.close",
            "pos.order.read", "pos.order.write", "pos.order.pay", "pos.order.finalize",
            "pos.receipt.read",
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
                    "hr.employee.read", "hr.employee.write", "hr.employee.archive",
                    "hr.department.read", "hr.department.write",
                    "hr.attendance.read", "hr.attendance.write",
                    "hr.time-off.read", "hr.time-off.write", "hr.time-off.approve",
                    "payroll.read", "payroll.write",
                    "expense.read", "expense.write", "expense.approve", "expense.post",
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
                    "hr.employee.read", "hr.employee.write", "hr.employee.archive",
                    "hr.department.read", "hr.department.write",
                    "hr.attendance.read", "hr.attendance.write",
                    "hr.time-off.read", "hr.time-off.write", "hr.time-off.approve",
                    "payroll.read", "payroll.write",
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
                    "hr.employee.read", "hr.employee.write", "hr.employee.archive",
                    "hr.department.read", "hr.department.write",
                    "hr.attendance.read", "hr.attendance.write",
                    "hr.time-off.read", "hr.time-off.write", "hr.time-off.approve",
                    "payroll.read", "payroll.write",
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
                    "hr.employee.read", "hr.department.read",
                    "hr.attendance.read",
                    "hr.time-off.read",
                    "payroll.read",
                    "expense.read",
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
                    "accounting.vendor-bill.read"),
            "EMPLOYEE", Set.of(
                    "hr.employee.self.read",
                    "hr.attendance.self.read",
                    "hr.time-off.self.read",
                    "hr.time-off.self.write",
                    "payroll.payslip.self.read",
                    "expense.read", "expense.write"),
            "HR_MANAGER", Set.of(
                    "hr.employee.read", "hr.employee.write", "hr.employee.archive",
                    "hr.department.read", "hr.department.write",
                    "hr.attendance.read", "hr.attendance.write",
                    "hr.time-off.read", "hr.time-off.write", "hr.time-off.approve",
                    "payroll.read", "payroll.write", "payroll.post", "payroll.pay",
                    "expense.read", "expense.write", "expense.approve", "expense.post")
    );

    private final PermissionJpaRepository permissionRepository;
    private final RoleJpaRepository roleRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final UserRoleJpaRepository userRoleRepository;

    public CompanyRoleProvisioner(PermissionJpaRepository permissionRepository,
                                  RoleJpaRepository roleRepository,
                                  RolePermissionJpaRepository rolePermissionRepository,
                                  UserRoleJpaRepository userRoleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public List<String> permissionCatalog() {
        return PERMISSION_CATALOG;
    }

    @Transactional
    public void provisionRoles(UUID companyId) {
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

    @Transactional
    public void grantAdminToUser(UUID companyId, UUID userId) {
        if (userId == null) {
            return;
        }
        RoleEntity admin = roleRepository.findByCompanyIdAndCode(companyId, "ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing for company " + companyId));
        UserRoleEntity.PK pk = new UserRoleEntity.PK(userId, admin.getId());
        if (!userRoleRepository.existsById(pk)) {
            userRoleRepository.save(new UserRoleEntity(userId, admin.getId()));
        }
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
