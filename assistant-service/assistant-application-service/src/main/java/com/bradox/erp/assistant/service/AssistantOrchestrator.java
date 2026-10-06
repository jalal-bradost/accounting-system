package com.bradox.erp.assistant.service;

import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.assistant.config.AiProperties;
import com.bradox.erp.assistant.conversation.ConversationStore;
import com.bradox.erp.assistant.provider.AIGenerationRequest;
import com.bradox.erp.assistant.provider.AIGenerationResult;
import com.bradox.erp.assistant.provider.AIProviderException;
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
import com.bradox.erp.platform.security.ForbiddenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssistantOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AssistantOrchestrator.class);

    private static final String SYSTEM_PROMPT = """
            You are the ERP assistant. You help authorized users explore ERP data using tools.
            Rules:
            - Never invent numbers, customers, products, dates, or accounting values.
            - Only use facts returned by tools. If tools return no data, say you could not determine it from ERP data.
            - Prefer calling tools immediately when the question maps to ERP figures. Do not skip answerable questions.
            - Always mention the date range and currency when answering financial questions.
            - If the user does not specify a period, use THIS_MONTH (do not ask them for a period).
            - Format money using the tool's currency code after the number (e.g. 17,710 IQD). Never use $ unless currency is USD. Never write "$X (IQD)" or similar.
            - When a question is mildly ambiguous, pick the best default tool, answer with that definition, and briefly note alternatives:
              • "how much did we make / profit?" → getProfitAndLoss (accounting net income)
              • "sales / revenue this month" → getSalesSummary (sales-order revenue; not P&L)
              • "product margin / which products are profitable?" → getProductProfit (realized by default)
              • "stock / on hand / do we have X?" → getStockOnHand
              • "what does customer X owe?" → getCustomerBalance
              • "what do we owe vendor X?" → getVendorBalance
              • "cash collected / money received" → getCashCollected
              • "overdue invoices / past due AR" → getOverdueInvoices
              • "open expense claims" → getExpenseSummary (workflow totals, not P&L expenses)
              • "how many products / employees / departments / customers / …" or company overview → getCompanyOverview
              • "list / find / show X" or "X in category/department Y" → lookupRecords
              • pending/draft orders, quotations, deliveries, receipts → getPipelineStatus
              • payroll / salaries / payslips → getPayrollSummary
              • cash or bank balances / cash on hand → getCashAndBankBalances
            - If a tool returns "restricted", tell the user they lack permission for that section.
            - Sales dashboard revenue is not the same as accounting P&L revenue.
            - Expense workflow totals are not the same as P&L expenses.
            - You cannot create, modify, or delete ERP records. Refuse write requests.
            - If no tool can answer, say clearly what is missing instead of giving a vague refusal.
            - Keep answers concise and business-clear.
            - Reply with only the final user-facing answer. Do not narrate tool use, analysis steps, or internal reasoning.
            - Reply in the same language the user used when possible.
            Today is %s.
            """;

    private final AiProperties properties;
    private final AiProviderRouter providerRouter;
    private final AssistantSettingsService settingsService;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final ConversationStore conversationStore;
    private final AuthorizationPort authorizationPort;
    private final CurrencyConversionPort currencyConversionPort;
    private final AuditLogPort auditLogPort;
    private final ObjectMapper objectMapper;

    public AssistantOrchestrator(AiProperties properties,
                                 AiProviderRouter providerRouter,
                                 AssistantSettingsService settingsService,
                                 ToolRegistry toolRegistry,
                                 ToolExecutor toolExecutor,
                                 ConversationStore conversationStore,
                                 AuthorizationPort authorizationPort,
                                 CurrencyConversionPort currencyConversionPort,
                                 AuditLogPort auditLogPort,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.providerRouter = providerRouter;
        this.settingsService = settingsService;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.conversationStore = conversationStore;
        this.authorizationPort = authorizationPort;
        this.currencyConversionPort = currencyConversionPort;
        this.auditLogPort = auditLogPort;
        this.objectMapper = objectMapper;
    }

    public ChatResponse chat(CompanyId companyId, UserId userId, UUID conversationId, String userMessage) {
        AiRuntimeSettings settings = settingsService.resolve(companyId);
        if (!settings.assistantEnabled()) {
            throw new AssistantUnavailableException(
                    "AI assistant is disabled. Enable it in Settings → AI Assistant.");
        }
        if (!settings.useGoogle() && !settings.useOpenAi() && !settings.useOllama()) {
            throw new AssistantUnavailableException(
                    "No AI provider is configured. Configure Google, OpenAI, and/or Ollama in Settings.");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }

        ConversationStore.Conversation conversation = conversationStore.getOrCreate(
                conversationId, userId != null ? userId.getId() : null, companyId.getId());

        String currency = safeCurrency(companyId.getId());
        List<ErpTool> allowedTools = permittedTools(userId);
        List<AIGenerationRequest.ToolDefinition> toolDefs = allowedTools.stream()
                .map(t -> new AIGenerationRequest.ToolDefinition(t.name(), t.description(), t.inputSchema()))
                .toList();

        List<AIGenerationRequest.Message> history = new ArrayList<>(conversation.snapshotMessages());
        if (history.isEmpty() || !"system".equals(history.get(0).role())) {
            history.add(0, AIGenerationRequest.Message.system(SYSTEM_PROMPT.formatted(LocalDate.now())));
        }
        history.add(AIGenerationRequest.Message.user(userMessage.trim()));

        List<Map<String, Object>> artifacts = new ArrayList<>();
        List<Map<String, Object>> toolTrace = new ArrayList<>();
        String reply = null;
        int rounds = 0;
        int maxRounds = Math.max(1, properties.getMaxToolRounds());

        try {
            while (rounds < maxRounds) {
                rounds++;
                long roundStarted = System.nanoTime();
                AIGenerationResult result = providerRouter.generate(new AIGenerationRequest(history, toolDefs), settings);
                long providerMs = (System.nanoTime() - roundStarted) / 1_000_000L;
                if (result.hasToolCalls()) {
                    long toolsStarted = System.nanoTime();
                    history.add(AIGenerationRequest.Message.assistant(result.getContent(), result.getToolCalls()));
                    for (AIGenerationRequest.ToolCall call : result.getToolCalls()) {
                        long toolStarted = System.nanoTime();
                        ToolResult toolResult = toolExecutor.execute(
                                call.name(),
                                call.argumentsJson(),
                                companyId,
                                userId,
                                conversation.getId(),
                                currency);
                        long toolMs = (System.nanoTime() - toolStarted) / 1_000_000L;
                        artifacts.addAll(toolResult.getArtifacts());
                        Map<String, Object> trace = new LinkedHashMap<>();
                        trace.put("tool", call.name());
                        trace.put("success", toolResult.isSuccess());
                        trace.put("durationMs", toolMs);
                        toolTrace.add(trace);
                        String payload = objectMapper.writeValueAsString(toolResult.toModelPayload());
                        history.add(AIGenerationRequest.Message.toolResult(call.id(), call.name(), payload));
                        log.info("Assistant round {} tool {} success={} {}ms",
                                rounds, call.name(), toolResult.isSuccess(), toolMs);
                    }
                    long toolsMs = (System.nanoTime() - toolsStarted) / 1_000_000L;
                    log.info("Assistant round {} provider={}ms tools={}ms toolCalls={}",
                            rounds, providerMs, toolsMs, result.getToolCalls().size());
                    continue;
                }
                reply = result.getContent() != null ? result.getContent().trim() : "";
                history.add(AIGenerationRequest.Message.assistant(reply, List.of()));
                log.info("Assistant round {} final provider={}ms replyChars={}",
                        rounds, providerMs, reply.length());
                break;
            }
            if (reply == null) {
                reply = "I reached the tool-call limit before finishing. Please narrow the question and try again.";
                history.add(AIGenerationRequest.Message.assistant(reply, List.of()));
            }
        } catch (ForbiddenException ex) {
            throw ex;
        } catch (AIProviderException ex) {
            throw new AssistantUnavailableException(ex.getMessage(), ex);
        } catch (Exception ex) {
            log.warn("Assistant chat failed: {}", ex.getMessage());
            throw new AssistantUnavailableException("The AI service is temporarily unavailable. Please try again.", ex);
        }

        conversation.replaceAll(history);
        auditChat(companyId, conversation.getId(), toolTrace.size());
        return new ChatResponse(conversation.getId(), reply, artifacts, toolTrace);
    }

    public void clearConversation(UUID conversationId, UserId userId) {
        conversationStore.delete(conversationId, userId != null ? userId.getId() : null);
    }

    private List<ErpTool> permittedTools(UserId userId) {
        List<ErpTool> allowed = new ArrayList<>();
        for (ErpTool tool : toolRegistry.all()) {
            if (userId == null) {
                continue;
            }
            if (authorizationPort.hasAny(userId, tool.requiredPermissions())) {
                allowed.add(tool);
            }
        }
        return allowed;
    }

    private String safeCurrency(UUID companyId) {
        try {
            String code = currencyConversionPort.baseCurrencyCode(companyId);
            return code != null && !code.isBlank() ? code : "IQD";
        } catch (Exception ex) {
            return "IQD";
        }
    }

    private void auditChat(CompanyId companyId, UUID conversationId, int toolCount) {
        try {
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("toolCount", toolCount);
            auditLogPort.recordBusinessEvent(
                    companyId,
                    "assistant.chat",
                    conversationId,
                    "AI assistant chat turn",
                    changes);
        } catch (Exception ex) {
            log.debug("Failed to audit assistant chat: {}", ex.getMessage());
        }
    }

    public record ChatResponse(
            UUID conversationId,
            String reply,
            List<Map<String, Object>> artifacts,
            List<Map<String, Object>> toolTrace
    ) {}
}
