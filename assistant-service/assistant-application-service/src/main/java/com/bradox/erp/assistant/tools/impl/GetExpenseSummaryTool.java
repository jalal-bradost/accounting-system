package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.expense.service.domain.dto.ExpenseSummaryResponse;
import com.bradox.erp.expense.service.domain.ports.input.ExpenseApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetExpenseSummaryTool implements ErpTool {

    private final ExpenseApplicationService expenseApplicationService;

    public GetExpenseSummaryTool(ExpenseApplicationService expenseApplicationService) {
        this.expenseApplicationService = expenseApplicationService;
    }

    @Override
    public String name() {
        return "getExpenseSummary";
    }

    @Override
    public String description() {
        return "Employee expense workflow totals currently open in the ERP: "
                + "to-submit, waiting-approval, and waiting-reimbursement amounts. "
                + "This is NOT the accounting P&L expense total for a calendar period — "
                + "use getProfitAndLoss for income-statement expenses.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("employeeId", Map.of(
                "type", "string",
                "description", "Optional employee UUID to filter; omit for company-wide open expense totals"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    @Override
    public String requiredPermission() {
        return "expense.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        UUID employeeId = null;
        if (arguments != null && arguments.hasNonNull("employeeId")) {
            String text = arguments.get("employeeId").asText();
            if (text != null && !text.isBlank()) {
                try {
                    employeeId = UUID.fromString(text.trim());
                } catch (IllegalArgumentException ex) {
                    return ToolResult.error("employeeId must be a valid UUID");
                }
            }
        }
        ExpenseSummaryResponse summary = expenseApplicationService.summary(context.companyId(), employeeId);
        String currency = context.currencyCode();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("toSubmitTotal", summary.getToSubmitTotal());
        data.put("waitingApprovalTotal", summary.getWaitingApprovalTotal());
        data.put("waitingReimbursementTotal", summary.getWaitingReimbursementTotal());
        data.put("currency", currency);
        data.put("note", "Open expense workflow buckets, not P&L period expenses");
        List<Map<String, Object>> artifacts = new ArrayList<>();
        artifacts.add(ToolResult.kpiArtifact("To submit", summary.getToSubmitTotal(), currency));
        artifacts.add(ToolResult.kpiArtifact("Waiting approval", summary.getWaitingApprovalTotal(), currency));
        artifacts.add(ToolResult.kpiArtifact("Waiting reimbursement", summary.getWaitingReimbursementTotal(), currency));
        return ToolResult.ok(data, artifacts);
    }
}
