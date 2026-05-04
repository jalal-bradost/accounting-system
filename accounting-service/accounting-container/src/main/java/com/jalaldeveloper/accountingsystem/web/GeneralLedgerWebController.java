package com.jalaldeveloper.accountingsystem.web;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.GeneralLedgerLine;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @deprecated The Thymeleaf {@code /web} UI is being replaced by the Next.js frontend in
 * {@code accounting-system-frontend}. Kept during the migration; will be removed once the
 * new client reaches feature parity. See {@code docs/MIGRATION.md}.
 */
@Deprecated(since = "0.2.0", forRemoval = true)
@Controller
@RequestMapping("/web/general-ledger")
public class GeneralLedgerWebController {

    private final ReportingApplicationService reportingApplicationService;
    private final AccountApplicationService accountApplicationService;
    private final WebCompanyContext companyContext;

    public GeneralLedgerWebController(ReportingApplicationService reportingApplicationService,
                                      AccountApplicationService accountApplicationService,
                                      WebCompanyContext companyContext) {
        this.reportingApplicationService = reportingApplicationService;
        this.accountApplicationService = accountApplicationService;
        this.companyContext = companyContext;
    }

    @GetMapping
    public String generalLedger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String accountId,
            Model model) {
        UUID companyId = companyContext.getCompanyId();
        model.addAttribute("pageTitle", "General Ledger");
        model.addAttribute("companyId", companyId);
        List<AccountResponse> accounts = accountApplicationService.listAccountsByCompany(companyId);
        model.addAttribute("allAccounts", accounts);
        if (from != null && to != null) {
            UUID accountUuid = parseAccountId(accountId);
            List<GeneralLedgerLine> lines = reportingApplicationService.getGeneralLedger(companyId, from, to, accountUuid);
            Map<UUID, AccountResponse> accountMap = accounts.stream().collect(Collectors.toMap(AccountResponse::getId, a -> a));
            model.addAttribute("from", from);
            model.addAttribute("to", to);
            model.addAttribute("accountId", accountUuid);
            model.addAttribute("lines", lines);
            model.addAttribute("accountMap", accountMap);
            return "general-ledger/result";
        }
        return "general-ledger/form";
    }

    private static UUID parseAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(accountId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
