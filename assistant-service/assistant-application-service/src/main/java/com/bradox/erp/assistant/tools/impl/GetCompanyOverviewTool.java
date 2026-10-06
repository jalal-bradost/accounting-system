package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.EntityAccess;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.erp.hr.service.domain.ports.input.DepartmentApplicationService;
import com.bradox.erp.hr.service.domain.ports.input.EmployeeApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.ProductApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.WarehouseApplicationService;
import com.bradox.erp.platform.settings.RoleApplicationService;
import com.bradox.erp.platform.settings.UserApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Permission-filtered counts across major ERP master-data areas.
 * Answers "how many products / employees / departments / …".
 */
@Component
public class GetCompanyOverviewTool implements ErpTool {

    public static final Set<String> ANY_OF = Set.of(
            "inventory.product.read",
            "inventory.warehouse.read",
            "contacts.partner.read",
            "hr.employee.read",
            "hr.department.read",
            "platform.user.read",
            "platform.role.read"
    );

    private final EntityAccess entityAccess;
    private final ProductApplicationService productService;
    private final WarehouseApplicationService warehouseService;
    private final PartnerApplicationService partnerService;
    private final EmployeeApplicationService employeeService;
    private final DepartmentApplicationService departmentService;
    private final UserApplicationService userService;
    private final RoleApplicationService roleService;

    public GetCompanyOverviewTool(EntityAccess entityAccess,
                                  ProductApplicationService productService,
                                  WarehouseApplicationService warehouseService,
                                  PartnerApplicationService partnerService,
                                  EmployeeApplicationService employeeService,
                                  DepartmentApplicationService departmentService,
                                  UserApplicationService userService,
                                  RoleApplicationService roleService) {
        this.entityAccess = entityAccess;
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.partnerService = partnerService;
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.userService = userService;
        this.roleService = roleService;
    }

    @Override
    public String name() {
        return "getCompanyOverview";
    }

    @Override
    public String description() {
        return "Company-wide counts for products (active/archived), product categories, warehouses, "
                + "customers, vendors, employees, departments, users, and roles. "
                + "Use for 'how many X do we have' / overview questions. Sections without permission are marked restricted.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of());
        return schema;
    }

    @Override
    public String requiredPermission() {
        return "inventory.product.read";
    }

    @Override
    public Set<String> requiredPermissions() {
        return ANY_OF;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        Map<String, Object> counts = new LinkedHashMap<>();
        List<List<Object>> tableRows = new ArrayList<>();

        putSection(counts, tableRows, "productsActive", "Active products",
                context, "inventory.product.read",
                () -> productService.searchProducts(context.companyId(), null, false, PageRequest.of(0, 1))
                        .getTotalElements());
        putSection(counts, tableRows, "productsIncludingArchived", "Products (incl. archived)",
                context, "inventory.product.read",
                () -> productService.searchProducts(context.companyId(), null, true, PageRequest.of(0, 1))
                        .getTotalElements());
        putSection(counts, tableRows, "productCategories", "Product categories",
                context, "inventory.product.read",
                () -> (long) productService.listCategories(context.companyId(), false).size());
        putSection(counts, tableRows, "warehouses", "Warehouses",
                context, "inventory.warehouse.read",
                () -> (long) warehouseService.listWarehouses(context.companyId(), false).size());
        putSection(counts, tableRows, "customers", "Customers",
                context, "contacts.partner.read",
                () -> partnerService.search(context.companyId(), null, true, null, false, PageRequest.of(0, 1))
                        .getTotalElements());
        putSection(counts, tableRows, "vendors", "Vendors",
                context, "contacts.partner.read",
                () -> partnerService.search(context.companyId(), null, null, true, false, PageRequest.of(0, 1))
                        .getTotalElements());
        putSection(counts, tableRows, "employees", "Employees",
                context, "hr.employee.read",
                () -> employeeService.search(context.companyId(), null, null, false, PageRequest.of(0, 1))
                        .getTotalElements());
        putSection(counts, tableRows, "departments", "Departments",
                context, "hr.department.read",
                () -> (long) departmentService.list(context.companyId(), false).size());
        putSection(counts, tableRows, "users", "Users",
                context, "platform.user.read",
                () -> userService.list(context.companyId().getId(), null, true, PageRequest.of(0, 1))
                        .getTotalElements());
        putSection(counts, tableRows, "roles", "Roles",
                context, "platform.role.read",
                () -> (long) roleService.list(context.companyId().getId()).size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("counts", counts);
        data.put("note", "Values equal to 'restricted' mean the current user lacks permission for that section.");
        return ToolResult.ok(data, List.of(ToolResult.tableArtifact(
                "Company overview",
                List.of("Section", "Count"),
                tableRows)));
    }

    private void putSection(Map<String, Object> counts,
                            List<List<Object>> tableRows,
                            String key,
                            String label,
                            ToolContext context,
                            String permission,
                            java.util.function.Supplier<Long> supplier) {
        Object value = entityAccess.whenPermitted(context.userId(), permission, supplier::get);
        counts.put(key, value);
        if (EntityAccess.RESTRICTED.equals(value)) {
            tableRows.add(List.of(label, EntityAccess.RESTRICTED));
        } else {
            long n = value instanceof Number num ? num.longValue() : 0L;
            tableRows.add(List.of(label, n));
        }
    }
}
