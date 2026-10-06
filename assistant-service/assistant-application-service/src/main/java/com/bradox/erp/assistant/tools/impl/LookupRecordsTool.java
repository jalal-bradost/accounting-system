package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.EntityAccess;
import com.bradox.erp.assistant.tools.support.ToolSchemas;
import com.bradox.erp.contacts.service.domain.dto.PartnerResponse;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.erp.expense.service.domain.dto.ExpenseTypeResponse;
import com.bradox.erp.expense.service.domain.ports.input.ExpenseTypeApplicationService;
import com.bradox.erp.hr.service.domain.dto.DepartmentSummaryResponse;
import com.bradox.erp.hr.service.domain.dto.EmployeeSummaryResponse;
import com.bradox.erp.hr.service.domain.ports.input.DepartmentApplicationService;
import com.bradox.erp.hr.service.domain.ports.input.EmployeeApplicationService;
import com.bradox.erp.inventory.service.domain.dto.ProductCategoryResponse;
import com.bradox.erp.inventory.service.domain.dto.ProductResponse;
import com.bradox.erp.inventory.service.domain.dto.UomCategoryResponse;
import com.bradox.erp.inventory.service.domain.dto.UomResponse;
import com.bradox.erp.inventory.service.domain.dto.WarehouseResponse;
import com.bradox.erp.inventory.service.domain.ports.input.ProductApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.UomApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.WarehouseApplicationService;
import com.bradox.erp.platform.settings.RoleApplicationService;
import com.bradox.erp.platform.settings.RoleResponse;
import com.bradox.erp.platform.settings.UserApplicationService;
import com.bradox.erp.platform.settings.UserResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LookupRecordsTool implements ErpTool {

    private static final Set<String> ENTITIES = Set.of(
            "products", "categories", "employees", "departments",
            "customers", "vendors", "warehouses", "expenseTypes", "users", "roles", "uoms"
    );

    public static final Set<String> ANY_OF = Set.of(
            "inventory.product.read",
            "inventory.warehouse.read",
            "inventory.uom.read",
            "contacts.partner.read",
            "hr.employee.read",
            "hr.department.read",
            "expense.read",
            "platform.user.read",
            "platform.role.read"
    );

    private final EntityAccess entityAccess;
    private final ProductApplicationService productService;
    private final WarehouseApplicationService warehouseService;
    private final UomApplicationService uomService;
    private final PartnerApplicationService partnerService;
    private final EmployeeApplicationService employeeService;
    private final DepartmentApplicationService departmentService;
    private final ExpenseTypeApplicationService expenseTypeService;
    private final UserApplicationService userService;
    private final RoleApplicationService roleService;

    public LookupRecordsTool(EntityAccess entityAccess,
                             ProductApplicationService productService,
                             WarehouseApplicationService warehouseService,
                             UomApplicationService uomService,
                             PartnerApplicationService partnerService,
                             EmployeeApplicationService employeeService,
                             DepartmentApplicationService departmentService,
                             ExpenseTypeApplicationService expenseTypeService,
                             UserApplicationService userService,
                             RoleApplicationService roleService) {
        this.entityAccess = entityAccess;
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.uomService = uomService;
        this.partnerService = partnerService;
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.expenseTypeService = expenseTypeService;
        this.userService = userService;
        this.roleService = roleService;
    }

    @Override
    public String name() {
        return "lookupRecords";
    }

    @Override
    public String description() {
        return "List or search master-data records. entity must be one of: products, categories, employees, "
                + "departments, customers, vendors, warehouses, expenseTypes, users, roles, uoms. "
                + "Optional query text, limit (default 10, max 25), and filters: departmentName, categoryName, departmentId, categoryId. "
                + "Use for 'list X', 'find X', 'employees in department Y', 'products in category Z'.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("entity", Map.of(
                "type", "string",
                "description", "One of: products, categories, employees, departments, "
                        + "customers, vendors, warehouses, expenseTypes, users, roles, uoms"));
        properties.put("query", Map.of("type", "string", "description", "Optional search text"));
        properties.put("limit", Map.of("type", "integer", "description", "Max rows (default 10, max 25)"));
        properties.put("departmentName", Map.of("type", "string", "description", "Filter employees by department name"));
        properties.put("departmentId", Map.of("type", "string", "description", "Filter employees by department UUID"));
        properties.put("categoryName", Map.of("type", "string", "description", "Filter products by category name"));
        properties.put("categoryId", Map.of("type", "string", "description", "Filter products by category UUID"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("entity"));
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
        String entity = ToolSchemas.text(arguments, "entity");
        if (entity == null) {
            return ToolResult.error("entity is required");
        }
        entity = entity.trim();
        if (!ENTITIES.contains(entity)) {
            return ToolResult.error("Unknown entity '" + entity + "'. Allowed: " + String.join(", ", ENTITIES));
        }
        String permission = permissionFor(entity);
        if (!entityAccess.can(context.userId(), permission)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("entity", entity);
            data.put("status", EntityAccess.RESTRICTED);
            data.put("message", "Missing permission: " + permission);
            return ToolResult.ok(data);
        }

        String query = ToolSchemas.text(arguments, "query");
        int limit = ToolSchemas.limit(arguments, 10, 25);

        return switch (entity) {
            case "products" -> lookupProducts(context, arguments, query, limit);
            case "categories" -> lookupCategories(context, query, limit);
            case "employees" -> lookupEmployees(context, arguments, query, limit);
            case "departments" -> lookupDepartments(context, query, limit);
            case "customers" -> lookupPartners(context, query, limit, true, null);
            case "vendors" -> lookupPartners(context, query, limit, null, true);
            case "warehouses" -> lookupWarehouses(context, query, limit);
            case "expenseTypes" -> lookupExpenseTypes(context, query, limit);
            case "users" -> lookupUsers(context, query, limit);
            case "roles" -> lookupRoles(context, query, limit);
            case "uoms" -> lookupUoms(context, query, limit);
            default -> ToolResult.error("Unsupported entity: " + entity);
        };
    }

    private static String permissionFor(String entity) {
        return switch (entity) {
            case "products", "categories" -> "inventory.product.read";
            case "warehouses" -> "inventory.warehouse.read";
            case "uoms" -> "inventory.uom.read";
            case "customers", "vendors" -> "contacts.partner.read";
            case "employees" -> "hr.employee.read";
            case "departments" -> "hr.department.read";
            case "expenseTypes" -> "expense.read";
            case "users" -> "platform.user.read";
            case "roles" -> "platform.role.read";
            default -> "platform.assistant.use";
        };
    }

    private ToolResult lookupProducts(ToolContext context, JsonNode args, String query, int limit) {
        UUID categoryId = parseUuid(ToolSchemas.text(args, "categoryId"));
        String categoryName = ToolSchemas.text(args, "categoryName");
        if (categoryId == null && categoryName != null) {
            categoryId = productService.listCategories(context.companyId(), false).stream()
                    .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(categoryName))
                    .map(ProductCategoryResponse::getId)
                    .findFirst()
                    .orElse(null);
            if (categoryId == null) {
                return ToolResult.error("No product category matching '" + categoryName + "'");
            }
        }
        // Fetch a bit more when filtering by category client-side
        int fetch = categoryId != null ? Math.min(limit * 5, 100) : limit;
        Page<ProductResponse> page = productService.searchProducts(
                context.companyId(), query, false, PageRequest.of(0, fetch));
        UUID finalCategoryId = categoryId;
        List<ProductResponse> filtered = page.getContent().stream()
                .filter(p -> finalCategoryId == null || finalCategoryId.equals(p.getCategoryId()))
                .limit(limit)
                .toList();
        long total = categoryId == null ? page.getTotalElements() : filtered.size();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        for (ProductResponse p : filtered) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId().toString());
            row.put("sku", nz(p.getSku()));
            row.put("name", nz(p.getName()));
            row.put("productType", p.getProductType() != null ? p.getProductType().name() : "");
            row.put("categoryId", p.getCategoryId() != null ? p.getCategoryId().toString() : "");
            row.put("listPrice", p.getListPrice());
            row.put("active", p.isActive());
            rows.add(row);
            table.add(List.of(nz(p.getSku()), nz(p.getName()),
                    p.getProductType() != null ? p.getProductType().name() : "",
                    p.getListPrice() != null ? p.getListPrice() : ""));
        }
        return listResult("products", rows, table, List.of("SKU", "Name", "Type", "List price"), total);
    }

    private ToolResult lookupCategories(ToolContext context, String query, int limit) {
        List<ProductCategoryResponse> all = productService.listCategories(context.companyId(), false);
        List<ProductCategoryResponse> filtered = filterByName(all, query, ProductCategoryResponse::getName, limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        for (ProductCategoryResponse c : filtered) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId().toString());
            row.put("name", nz(c.getName()));
            row.put("parentId", c.getParentId() != null ? c.getParentId().toString() : "");
            row.put("active", c.isActive());
            rows.add(row);
            table.add(List.of(nz(c.getName()), c.isActive() ? "active" : "inactive"));
        }
        long total = query == null || query.isBlank()
                ? all.size()
                : all.stream().filter(c -> containsIgnoreCase(c.getName(), query)).count();
        return listResult("categories", rows, table, List.of("Name", "Status"), total);
    }

    private ToolResult lookupEmployees(ToolContext context, JsonNode args, String query, int limit) {
        UUID departmentId = parseUuid(ToolSchemas.text(args, "departmentId"));
        String departmentName = ToolSchemas.text(args, "departmentName");
        if (departmentId == null && departmentName != null) {
            departmentId = departmentService.list(context.companyId(), false).stream()
                    .filter(d -> d.name() != null && d.name().equalsIgnoreCase(departmentName))
                    .map(DepartmentSummaryResponse::id)
                    .findFirst()
                    .orElse(null);
            if (departmentId == null) {
                return ToolResult.error("No department matching '" + departmentName + "'");
            }
        }
        Page<EmployeeSummaryResponse> page = employeeService.search(
                context.companyId(), query, departmentId, false, PageRequest.of(0, limit));
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        for (EmployeeSummaryResponse e : page.getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.id().toString());
            row.put("displayName", nz(e.displayName()));
            row.put("jobTitle", nz(e.jobTitle()));
            row.put("departmentName", nz(e.departmentName()));
            row.put("workEmail", nz(e.workEmail()));
            row.put("active", e.active());
            rows.add(row);
            table.add(List.of(nz(e.displayName()), nz(e.jobTitle()), nz(e.departmentName())));
        }
        return listResult("employees", rows, table, List.of("Name", "Job title", "Department"), page.getTotalElements());
    }

    private ToolResult lookupDepartments(ToolContext context, String query, int limit) {
        List<DepartmentSummaryResponse> all = departmentService.list(context.companyId(), false);
        List<DepartmentSummaryResponse> filtered = filterByName(all, query, DepartmentSummaryResponse::name, limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        for (DepartmentSummaryResponse d : filtered) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", d.id().toString());
            row.put("name", nz(d.name()));
            row.put("managerName", nz(d.managerName()));
            row.put("employeeCount", d.employeeCount());
            row.put("active", d.active());
            rows.add(row);
            table.add(List.of(nz(d.name()), nz(d.managerName()), d.employeeCount()));
        }
        long total = query == null || query.isBlank()
                ? all.size()
                : all.stream().filter(d -> containsIgnoreCase(d.name(), query)).count();
        return listResult("departments", rows, table, List.of("Name", "Manager", "Employees"), total);
    }

    private ToolResult lookupPartners(ToolContext context, String query, int limit,
                                      Boolean isCustomer, Boolean isVendor) {
        Page<PartnerResponse> page = partnerService.search(
                context.companyId(), query, isCustomer, isVendor, false, PageRequest.of(0, limit));
        String entity = Boolean.TRUE.equals(isCustomer) ? "customers" : "vendors";
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        for (PartnerResponse p : page.getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId().toString());
            row.put("displayName", nz(p.getDisplayName()));
            row.put("email", nz(p.getEmail()));
            row.put("phone", nz(p.getPhone()));
            row.put("isCustomer", p.isCustomer());
            row.put("isVendor", p.isVendor());
            row.put("active", p.isActive());
            rows.add(row);
            table.add(List.of(nz(p.getDisplayName()), nz(p.getEmail()), nz(p.getPhone())));
        }
        return listResult(entity, rows, table, List.of("Name", "Email", "Phone"), page.getTotalElements());
    }

    private ToolResult lookupWarehouses(ToolContext context, String query, int limit) {
        List<WarehouseResponse> all = warehouseService.listWarehouses(context.companyId(), false);
        List<WarehouseResponse> filtered = filterByName(all, query, WarehouseResponse::getName, limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        for (WarehouseResponse w : filtered) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", w.getId().toString());
            row.put("code", nz(w.getCode()));
            row.put("name", nz(w.getName()));
            row.put("active", w.isActive());
            rows.add(row);
            table.add(List.of(nz(w.getCode()), nz(w.getName())));
        }
        long total = query == null || query.isBlank()
                ? all.size()
                : all.stream().filter(w -> containsIgnoreCase(w.getName(), query)
                || containsIgnoreCase(w.getCode(), query)).count();
        return listResult("warehouses", rows, table, List.of("Code", "Name"), total);
    }

    private ToolResult lookupExpenseTypes(ToolContext context, String query, int limit) {
        List<ExpenseTypeResponse> all = expenseTypeService.list(context.companyId());
        List<ExpenseTypeResponse> filtered = filterByName(all, query, ExpenseTypeResponse::getName, limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        for (ExpenseTypeResponse t : filtered) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", t.getId().toString());
            row.put("name", nz(t.getName()));
            row.put("active", t.isActive());
            rows.add(row);
            table.add(List.of(nz(t.getName()), t.isActive() ? "active" : "inactive"));
        }
        long total = query == null || query.isBlank()
                ? all.size()
                : all.stream().filter(t -> containsIgnoreCase(t.getName(), query)).count();
        return listResult("expenseTypes", rows, table, List.of("Name", "Status"), total);
    }

    private ToolResult lookupUsers(ToolContext context, String query, int limit) {
        Page<UserResponse> page = userService.list(
                context.companyId().getId(), query, true, PageRequest.of(0, limit));
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        for (UserResponse u : page.getContent()) {
            String roles = u.roles() == null ? "" : u.roles().stream()
                    .map(UserResponse.RoleSummary::name)
                    .collect(Collectors.joining(", "));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.id().toString());
            row.put("username", nz(u.username()));
            row.put("displayName", nz(u.displayName()));
            row.put("email", nz(u.email()));
            row.put("active", u.active());
            row.put("roles", roles);
            rows.add(row);
            table.add(List.of(nz(u.displayName()), nz(u.username()), roles));
        }
        return listResult("users", rows, table, List.of("Name", "Username", "Roles"), page.getTotalElements());
    }

    private ToolResult lookupRoles(ToolContext context, String query, int limit) {
        List<RoleResponse> all = roleService.list(context.companyId().getId());
        List<RoleResponse> filtered = filterByName(all, query, RoleResponse::name, limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        for (RoleResponse r : filtered) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.id().toString());
            row.put("code", nz(r.code()));
            row.put("name", nz(r.name()));
            row.put("userCount", r.userCount());
            row.put("active", r.active());
            rows.add(row);
            table.add(List.of(nz(r.code()), nz(r.name()), r.userCount()));
        }
        long total = query == null || query.isBlank()
                ? all.size()
                : all.stream().filter(r -> containsIgnoreCase(r.name(), query)
                || containsIgnoreCase(r.code(), query)).count();
        return listResult("roles", rows, table, List.of("Code", "Name", "Users"), total);
    }

    private ToolResult lookupUoms(ToolContext context, String query, int limit) {
        List<UomCategoryResponse> categories = uomService.listUomCategories(context.companyId(), false);
        List<UomResponse> all = new ArrayList<>();
        for (UomCategoryResponse cat : categories) {
            all.addAll(uomService.listUomsByCategory(cat.getId(), false));
        }
        List<UomResponse> filtered = filterByName(all, query, UomResponse::getName, limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        for (UomResponse u : filtered) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.getId().toString());
            row.put("name", nz(u.getName()));
            row.put("categoryId", u.getCategoryId() != null ? u.getCategoryId().toString() : "");
            row.put("uomType", u.getUomType() != null ? u.getUomType().name() : "");
            row.put("active", u.isActive());
            rows.add(row);
            table.add(List.of(nz(u.getName()), u.getUomType() != null ? u.getUomType().name() : ""));
        }
        long total = query == null || query.isBlank()
                ? all.size()
                : all.stream().filter(u -> containsIgnoreCase(u.getName(), query)).count();
        return listResult("uoms", rows, table, List.of("Name", "Type"), total);
    }

    private static ToolResult listResult(String entity,
                                         List<Map<String, Object>> rows,
                                         List<List<Object>> table,
                                         List<String> columns,
                                         long total) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entity", entity);
        data.put("total", total);
        data.put("returned", rows.size());
        data.put("records", rows);
        return ToolResult.ok(data, List.of(
                ToolResult.tableArtifact(entity + " (" + total + " total)", columns, table, total)));
    }

    private static <T> List<T> filterByName(List<T> all, String query,
                                            java.util.function.Function<T, String> nameFn, int limit) {
        return all.stream()
                .filter(item -> query == null || query.isBlank() || containsIgnoreCase(nameFn.apply(item), query))
                .limit(limit)
                .toList();
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null) {
            return false;
        }
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static UUID parseUuid(String text) {
        if (text == null) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String nz(String v) {
        return v != null ? v : "";
    }
}
