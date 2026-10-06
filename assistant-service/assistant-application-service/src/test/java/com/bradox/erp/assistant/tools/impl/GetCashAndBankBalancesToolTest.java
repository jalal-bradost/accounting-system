package com.bradox.erp.assistant.tools.impl;

import com.bradox.erp.accounting.service.domain.create.JournalResponse;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
import com.bradox.erp.assistant.tools.ErpTool;
import com.bradox.erp.assistant.tools.ToolResult;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.JournalType;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetCashAndBankBalancesToolTest {

    @Test
    void sumsCashAndBankBalances() {
        UUID companyUuid = UUID.randomUUID();
        CompanyId companyId = new CompanyId(companyUuid);
        UUID cashJournalId = UUID.randomUUID();
        UUID bankJournalId = UUID.randomUUID();
        UUID cashAccountId = UUID.randomUUID();
        UUID bankAccountId = UUID.randomUUID();

        JournalApplicationService journals = mock(JournalApplicationService.class);
        when(journals.listJournalsByCompany(companyUuid)).thenReturn(List.of(
                new JournalResponse(cashJournalId, companyUuid, "CASH", "Cash", JournalType.CASH),
                new JournalResponse(bankJournalId, companyUuid, "BANK", "Bank", JournalType.BANK),
                new JournalResponse(UUID.randomUUID(), companyUuid, "SALE", "Sales", JournalType.SALE)
        ));

        AccountingReferenceLookupPort lookup = mock(AccountingReferenceLookupPort.class);
        when(lookup.resolveLiquidityAccountIdForJournal(companyUuid, cashJournalId)).thenReturn(cashAccountId);
        when(lookup.resolveLiquidityAccountIdForJournal(companyUuid, bankJournalId)).thenReturn(bankAccountId);

        AccountBalanceRepository balances = mock(AccountBalanceRepository.class);
        when(balances.getBalancesUpTo(eq(companyId), any(), eq(List.of(AccountType.BANK_AND_CASH))))
                .thenReturn(List.of(
                        new AccountBalanceRepository.AccountBalanceLine(cashAccountId, new BigDecimal("1000")),
                        new AccountBalanceRepository.AccountBalanceLine(bankAccountId, new BigDecimal("5000"))
                ));

        GetCashAndBankBalancesTool tool = new GetCashAndBankBalancesTool(journals, lookup, balances);
        ErpTool.ToolContext ctx = new ErpTool.ToolContext(
                companyId, new UserId(UUID.randomUUID()), UUID.randomUUID(), "IQD");
        ToolResult result = tool.execute(ctx, new ObjectMapper().createObjectNode());

        assertThat(result.isSuccess()).isTrue();
        assertThat((BigDecimal) result.getData().get("totalCash")).isEqualByComparingTo("1000");
        assertThat((BigDecimal) result.getData().get("totalBank")).isEqualByComparingTo("5000");
        assertThat((BigDecimal) result.getData().get("total")).isEqualByComparingTo("6000");
    }
}
