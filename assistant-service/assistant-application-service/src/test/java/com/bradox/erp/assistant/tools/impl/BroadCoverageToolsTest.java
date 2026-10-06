package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.config.AiProperties;
import com.bradox.erp.assistant.dates.RelativeDateResolver;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.assistant.tools.support.EntityAccess;
import com.bradox.erp.contacts.service.domain.ports.input.PartnerApplicationService;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.hr.service.domain.dto.DepartmentSummaryResponse;
import com.bradox.erp.hr.service.domain.dto.EmployeeSummaryResponse;
import com.bradox.erp.hr.service.domain.dto.payroll.PayrollApi;
import com.bradox.erp.hr.service.domain.ports.input.DepartmentApplicationService;
import com.bradox.erp.hr.service.domain.ports.input.EmployeeApplicationService;
import com.bradox.erp.hr.service.domain.ports.input.PayRunApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.ProductApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.UomApplicationService;
import com.bradox.erp.inventory.service.domain.ports.input.WarehouseApplicationService;
import com.bradox.erp.platform.security.AuthorizationPort;
import com.bradox.erp.platform.settings.RoleApplicationService;
import com.bradox.erp.platform.settings.UserApplicationService;
import com.bradox.erp.purchase.service.domain.ports.input.PurchaseApplicationService;
import com.bradox.erp.sales.domain.core.SalesOrderState;
import com.bradox.erp.sales.service.domain.ports.input.SalesApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BroadCoverageToolsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthorizationPort authorizationPort;
    private EntityAccess entityAccess;
    private RelativeDateResolver dateResolver;
    private CompanyId companyId;
    private UserId userId;
    private ErpTool.ToolContext ctx;

    @BeforeEach
    void setUp() {
        authorizationPort = mock(AuthorizationPort.class);
        entityAccess = new EntityAccess(authorizationPort);
        AiProperties props = new AiProperties();
        props.setMaxDateRangeDays(400);
        dateResolver = new RelativeDateResolver(props);
        companyId = new CompanyId(UUID.randomUUID());
        userId = new UserId(UUID.randomUUID());
        ctx = new ErpTool.ToolContext(companyId, userId, UUID.randomUUID(), "IQD");
        when(authorizationPort.hasAny(eq(userId), any())).thenReturn(true);
    }

    @Test
    void companyOverviewMarksRestrictedSections() {
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("inventory.product.read")))).thenReturn(true);
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("hr.employee.read")))).thenReturn(false);
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("hr.department.read")))).thenReturn(false);
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("inventory.warehouse.read")))).thenReturn(false);
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("contacts.partner.read")))).thenReturn(false);
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("salespeople.read")))).thenReturn(false);
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("drivers.read")))).thenReturn(false);
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("platform.user.read")))).thenReturn(false);
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("platform.role.read")))).thenReturn(false);

        ProductApplicationService products = mock(ProductApplicationService.class);
        when(products.searchProducts(eq(companyId), isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(1), 42));
        when(products.listCategories(eq(companyId), anyBoolean())).thenReturn(List.of());

        GetCompanyOverviewTool tool = new GetCompanyOverviewTool(
                entityAccess, products,
                mock(WarehouseApplicationService.class),
                mock(PartnerApplicationService.class),
                mock(EmployeeApplicationService.class),
                mock(DepartmentApplicationService.class),
                mock(UserApplicationService.class),
                mock(RoleApplicationService.class));

        ToolResult result = tool.execute(ctx, objectMapper.createObjectNode());
        assertThat(result.isSuccess()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> counts = (Map<String, Object>) result.getData().get("counts");
        assertThat(counts.get("productsActive")).isEqualTo(42L);
        assertThat(counts.get("employees")).isEqualTo(EntityAccess.RESTRICTED);
    }

    @Test
    void lookupRecordsCapsLimitAndRequiresEntity() {
        EmployeeApplicationService employees = mock(EmployeeApplicationService.class);
        DepartmentApplicationService departments = mock(DepartmentApplicationService.class);
        UUID deptId = UUID.randomUUID();
        when(departments.list(eq(companyId), anyBoolean())).thenReturn(List.of(
                new DepartmentSummaryResponse(deptId, "Sales", null, null, null, null, 0, 3, true)));
        EmployeeSummaryResponse emp = new EmployeeSummaryResponse(
                UUID.randomUUID(), companyId.getId(), "Ali", null, null, "Rep",
                deptId, "Sales", null, null, null, null, null, true);
        when(employees.search(eq(companyId), isNull(), eq(deptId), eq(false), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable pageable = inv.getArgument(4);
                    assertThat(pageable.getPageSize()).isEqualTo(25);
                    return new PageImpl<>(List.of(emp), pageable, 1);
                });

        LookupRecordsTool tool = new LookupRecordsTool(
                entityAccess,
                mock(ProductApplicationService.class),
                mock(WarehouseApplicationService.class),
                mock(UomApplicationService.class),
                mock(PartnerApplicationService.class),
                employees,
                departments,
                mock(com.bradox.erp.expense.service.domain.ports.input.ExpenseTypeApplicationService.class),
                mock(UserApplicationService.class),
                mock(RoleApplicationService.class));

        ObjectNode args = objectMapper.createObjectNode();
        args.put("entity", "employees");
        args.put("departmentName", "Sales");
        args.put("limit", 100);
        ToolResult result = tool.execute(ctx, args);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().get("total")).isEqualTo(1L);
        assertThat(result.getData().get("returned")).isEqualTo(1);
    }

    @Test
    void lookupRecordsReturnsRestrictedWithoutPermission() {
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("hr.employee.read")))).thenReturn(false);
        LookupRecordsTool tool = new LookupRecordsTool(
                entityAccess,
                mock(ProductApplicationService.class),
                mock(WarehouseApplicationService.class),
                mock(UomApplicationService.class),
                mock(PartnerApplicationService.class),
                mock(EmployeeApplicationService.class),
                mock(DepartmentApplicationService.class),
                mock(com.bradox.erp.expense.service.domain.ports.input.ExpenseTypeApplicationService.class),
                mock(UserApplicationService.class),
                mock(RoleApplicationService.class));
        ObjectNode args = objectMapper.createObjectNode();
        args.put("entity", "employees");
        ToolResult result = tool.execute(ctx, args);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().get("status")).isEqualTo(EntityAccess.RESTRICTED);
    }

    @Test
    void pipelineStatusCountsSalesByState() {
        SalesApplicationService sales = mock(SalesApplicationService.class);
        when(sales.searchSalesOrders(eq(companyId.getId()), any(SalesOrderState.class),
                isNull(), isNull(), any(Pageable.class)))
                .thenAnswer(inv -> {
                    SalesOrderState state = inv.getArgument(1);
                    long total = state == SalesOrderState.DRAFT ? 3L : 0L;
                    return new PageImpl<>(List.of(), Pageable.ofSize(1), total);
                });

        GetPipelineStatusTool tool = new GetPipelineStatusTool(
                entityAccess, sales,
                mock(PurchaseApplicationService.class),
                mock(StockPickingApplicationService.class));
        ObjectNode args = objectMapper.createObjectNode();
        args.put("kind", "sales");
        ToolResult result = tool.execute(ctx, args);
        assertThat(result.isSuccess()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Long> salesOrders = (Map<String, Long>) result.getData().get("salesOrders");
        assertThat(salesOrders.get("DRAFT")).isEqualTo(3L);
    }

    @Test
    void payrollSummaryFiltersByPeriodOverlap() {
        PayRunApplicationService payRuns = mock(PayRunApplicationService.class);
        LocalDate today = LocalDate.now();
        when(payRuns.listRuns(companyId)).thenReturn(List.of(
                new PayrollApi.PayRunSummaryResponse(
                        UUID.randomUUID(), "Jan", today.withDayOfMonth(1), today,
                        "PAID", 2, new BigDecimal("500")),
                new PayrollApi.PayRunSummaryResponse(
                        UUID.randomUUID(), "Old", today.minusYears(2).withDayOfMonth(1),
                        today.minusYears(2).withDayOfMonth(28), "PAID", 1, new BigDecimal("99"))
        ));

        GetPayrollSummaryTool tool = new GetPayrollSummaryTool(payRuns, dateResolver);
        ObjectNode args = objectMapper.createObjectNode();
        args.put("period", "THIS_MONTH");
        ToolResult result = tool.execute(ctx, args);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().get("payRunCount")).isEqualTo(1);
        assertThat((BigDecimal) result.getData().get("totalNet")).isEqualByComparingTo(new BigDecimal("500"));
    }
}
