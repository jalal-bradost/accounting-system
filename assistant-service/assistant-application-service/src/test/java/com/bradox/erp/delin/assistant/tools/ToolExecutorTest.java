package com.bradox.erp.assistant.tools;

import com.bradox.erp.assistant.config.AiProperties;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.platform.security.AuthorizationPort;
import com.bradox.erp.platform.security.ForbiddenException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolExecutorTest {

    private AuthorizationPort authorizationPort;
    private AuditLogPort auditLogPort;
    private ErpTool tool;
    private ToolExecutor executor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authorizationPort = mock(AuthorizationPort.class);
        auditLogPort = mock(AuditLogPort.class);
        tool = new ErpTool() {
            @Override
            public String name() {
                return "getProfitAndLoss";
            }

            @Override
            public String description() {
                return "test";
            }

            @Override
            public Map<String, Object> inputSchema() {
                return Map.of("type", "object");
            }

            @Override
            public String requiredPermission() {
                return "accounting.report.read";
            }

            @Override
            public ToolResult execute(ToolContext context, JsonNode arguments) {
                return ToolResult.ok(Map.of("netIncome", 100));
            }
        };
        AiProperties props = new AiProperties();
        props.setEnabled(true);
        executor = new ToolExecutor(
                new ToolRegistry(List.of(tool)),
                authorizationPort,
                auditLogPort,
                objectMapper,
                props);
    }

    @Test
    void deniesWhenMissingDomainPermission() {
        UserId userId = new UserId(UUID.randomUUID());
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("accounting.report.read")))).thenReturn(false);

        assertThatThrownBy(() -> executor.execute(
                "getProfitAndLoss",
                "{\"period\":\"THIS_MONTH\"}",
                new CompanyId(UUID.randomUUID()),
                userId,
                UUID.randomUUID(),
                "IQD"))
                .isInstanceOf(ForbiddenException.class);

        verify(auditLogPort).recordBusinessEvent(any(), eq("assistant.tool"), any(), any(), any());
    }

    @Test
    void executesWhenPermitted() {
        UserId userId = new UserId(UUID.randomUUID());
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("accounting.report.read")))).thenReturn(true);

        ToolResult result = executor.execute(
                "getProfitAndLoss",
                "{}",
                new CompanyId(UUID.randomUUID()),
                userId,
                UUID.randomUUID(),
                "IQD");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsEntry("netIncome", 100);
    }

    @Test
    void executesWhenAnyOfPermissionsMatch() {
        ErpTool multi = new ErpTool() {
            @Override public String name() { return "getCompanyOverview"; }
            @Override public String description() { return "overview"; }
            @Override public Map<String, Object> inputSchema() { return Map.of("type", "object"); }
            @Override public String requiredPermission() { return "inventory.product.read"; }
            @Override public Set<String> requiredPermissions() {
                return Set.of("inventory.product.read", "hr.employee.read");
            }
            @Override public ToolResult execute(ToolContext context, JsonNode arguments) {
                return ToolResult.ok(Map.of("ok", true));
            }
        };
        AiProperties props = new AiProperties();
        props.setEnabled(true);
        ToolExecutor multiExecutor = new ToolExecutor(
                new ToolRegistry(List.of(multi)),
                authorizationPort,
                auditLogPort,
                objectMapper,
                props);
        UserId userId = new UserId(UUID.randomUUID());
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("inventory.product.read", "hr.employee.read"))))
                .thenReturn(true);

        ToolResult result = multiExecutor.execute(
                "getCompanyOverview",
                "{}",
                new CompanyId(UUID.randomUUID()),
                userId,
                UUID.randomUUID(),
                "IQD");
        assertThat(result.isSuccess()).isTrue();
    }
}
