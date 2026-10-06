package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.accounting.service.domain.create.JournalResponse;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.JournalType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetCashAndBankBalancesTool implements ErpTool {

    private final JournalApplicationService journalService;
    private final AccountingReferenceLookupPort accountingReferenceLookupPort;
    private final AccountBalanceRepository accountBalanceRepository;

    public GetCashAndBankBalancesTool(JournalApplicationService journalService,
                                      AccountingReferenceLookupPort accountingReferenceLookupPort,
                                      AccountBalanceRepository accountBalanceRepository) {
        this.journalService = journalService;
        this.accountingReferenceLookupPort = accountingReferenceLookupPort;
        this.accountBalanceRepository = accountBalanceRepository;
    }

    @Override
    public String name() {
        return "getCashAndBankBalances";
    }

    @Override
    public String description() {
        return "Current balances for CASH and BANK journals (cash on hand / bank accounts). "
                + "Use for 'cash balance', 'bank balance', 'how much cash do we have'.";
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
        return "accounting.report.read";
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        UUID companyId = context.companyId().getId();
        String currency = context.currencyCode();
        LocalDate asOf = LocalDate.now();

        List<AccountBalanceRepository.AccountBalanceLine> balances =
                accountBalanceRepository.getBalancesUpTo(
                        context.companyId(), asOf, List.of(AccountType.BANK_AND_CASH));
        Map<UUID, BigDecimal> balanceByAccount = new LinkedHashMap<>();
        for (AccountBalanceRepository.AccountBalanceLine line : balances) {
            if (line.accountId() != null) {
                balanceByAccount.put(line.accountId(),
                        line.balance() != null ? line.balance() : BigDecimal.ZERO);
            }
        }

        List<Map<String, Object>> journals = new ArrayList<>();
        List<List<Object>> table = new ArrayList<>();
        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalBank = BigDecimal.ZERO;

        for (JournalResponse journal : journalService.listJournalsByCompany(companyId)) {
            JournalType type = journal.getJournalType();
            if (type != JournalType.CASH && type != JournalType.BANK) {
                continue;
            }
            UUID accountId = accountingReferenceLookupPort.resolveLiquidityAccountIdForJournal(
                    companyId, journal.getId());
            BigDecimal balance = accountId != null
                    ? balanceByAccount.getOrDefault(accountId, BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            if (type == JournalType.CASH) {
                totalCash = totalCash.add(balance);
            } else {
                totalBank = totalBank.add(balance);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("journalId", journal.getId().toString());
            row.put("code", journal.getCode() != null ? journal.getCode() : "");
            row.put("name", journal.getName() != null ? journal.getName() : "");
            row.put("type", type.name());
            row.put("balance", balance);
            row.put("accountId", accountId != null ? accountId.toString() : "");
            journals.add(row);
            table.add(List.of(
                    journal.getCode() != null ? journal.getCode() : "",
                    journal.getName() != null ? journal.getName() : "",
                    type.name(),
                    balance));
        }

        BigDecimal grandTotal = totalCash.add(totalBank);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("asOf", asOf.toString());
        data.put("currency", currency);
        data.put("totalCash", totalCash);
        data.put("totalBank", totalBank);
        data.put("total", grandTotal);
        data.put("journals", journals);

        List<Map<String, Object>> artifacts = new ArrayList<>();
        artifacts.add(ToolResult.kpiArtifact("Cash", totalCash, currency));
        artifacts.add(ToolResult.kpiArtifact("Bank", totalBank, currency));
        artifacts.add(ToolResult.kpiArtifact("Total liquidity", grandTotal, currency));
        artifacts.add(ToolResult.tableArtifact(
                "Cash & bank journals",
                List.of("Code", "Name", "Type", "Balance"),
                table,
                (long) journals.size()));
        return ToolResult.ok(data, artifacts);
    }
}
