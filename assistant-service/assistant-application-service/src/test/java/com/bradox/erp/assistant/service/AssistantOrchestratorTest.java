package com.bradox.erp.assistant.service;

import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.assistant.config.AiProperties;
import com.bradox.erp.assistant.conversation.ConversationStore;
import com.bradox.erp.assistant.provider.AIGenerationRequest;
import com.bradox.erp.assistant.provider.AIGenerationResult;
import com.bradox.erp.assistant.provider.AiProviderRouter;
import com.bradox.erp.assistant.settings.AiRuntimeSettings;
import com.bradox.erp.assistant.settings.AssistantSettingsService;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolExecutor;
import com.bradox.erp.assistant.tools.ToolRegistry;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.platform.security.AuthorizationPort;
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
import static org.mockito.Mockito.when;

class AssistantOrchestratorTest {

    private AiProperties properties;
    private AiProviderRouter providerRouter;
    private AssistantSettingsService settingsService;
    private AuthorizationPort authorizationPort;
    private ToolExecutor toolExecutor;
    private AssistantOrchestrator orchestrator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setEnabled(true);
        properties.setMaxToolRounds(3);
        properties.setMaxHistoryMessages(12);

        providerRouter = mock(AiProviderRouter.class);
        settingsService = mock(AssistantSettingsService.class);
        authorizationPort = mock(AuthorizationPort.class);
        CurrencyConversionPort currencyConversionPort = mock(CurrencyConversionPort.class);
        when(currencyConversionPort.baseCurrencyCode(any())).thenReturn("IQD");
        AuditLogPort auditLogPort = mock(AuditLogPort.class);
        toolExecutor = mock(ToolExecutor.class);

        ErpTool salesTool = new ErpTool() {
            @Override public String name() { return "getSalesSummary"; }
            @Override public String description() { return "sales"; }
            @Override public Map<String, Object> inputSchema() { return Map.of("type", "object"); }
            @Override public String requiredPermission() { return "sales.order.read"; }
            @Override public ToolResult execute(ToolContext context, JsonNode arguments) {
                return ToolResult.ok(Map.of());
            }
        };

        orchestrator = new AssistantOrchestrator(
                properties,
                providerRouter,
                settingsService,
                new ToolRegistry(List.of(salesTool)),
                toolExecutor,
                new ConversationStore(properties),
                authorizationPort,
                currencyConversionPort,
                auditLogPort,
                objectMapper);
    }

    @Test
    void refusesWhenDisabled() {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        when(settingsService.resolve(companyId)).thenReturn(new AiRuntimeSettings(
                false, "hybrid", false, null, "gemini-3.8-flash",
                false, null, "gpt-4o-mini",
                true, "http://localhost:11434", "qwen2.5:3b"));
        assertThatThrownBy(() -> orchestrator.chat(
                companyId,
                new UserId(UUID.randomUUID()),
                null,
                "What was revenue?"))
                .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void runsToolThenReturnsFinalAnswer() throws Exception {
        UserId userId = new UserId(UUID.randomUUID());
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        AiRuntimeSettings settings = new AiRuntimeSettings(
                true, "ollama", false, null, "gemini-3.8-flash",
                false, null, "gpt-4o-mini",
                true, "http://localhost:11434", "qwen2.5:3b");
        when(settingsService.resolve(companyId)).thenReturn(settings);
        when(authorizationPort.hasAny(eq(userId), eq(Set.of("sales.order.read")))).thenReturn(true);

        when(providerRouter.generate(any(), eq(settings)))
                .thenReturn(new AIGenerationResult(
                        null,
                        List.of(new AIGenerationRequest.ToolCall("c1", "getSalesSummary", "{\"period\":\"THIS_MONTH\"}")),
                        true))
                .thenReturn(new AIGenerationResult("Sales revenue was 10,000 IQD this month.", List.of(), true));

        when(toolExecutor.execute(eq("getSalesSummary"), any(), eq(companyId), eq(userId), any(), eq("IQD")))
                .thenReturn(ToolResult.ok(
                        Map.of("salesRevenue", 10000, "currency", "IQD"),
                        List.of(ToolResult.kpiArtifact("Sales revenue", 10000, "IQD"))));

        AssistantOrchestrator.ChatResponse response = orchestrator.chat(companyId, userId, null, "How much did we sell?");

        assertThat(response.reply()).contains("10,000");
        assertThat(response.artifacts()).isNotEmpty();
        assertThat(response.toolTrace()).hasSize(1);
        assertThat(response.conversationId()).isNotNull();
    }
}
