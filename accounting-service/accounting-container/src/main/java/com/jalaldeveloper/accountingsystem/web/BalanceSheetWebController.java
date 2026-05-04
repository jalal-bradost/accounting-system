package com.jalaldeveloper.accountingsystem.web;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.BalanceSheetReport;
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
@RequestMapping("/web/balance-sheet")
public class BalanceSheetWebController {

    private final ReportingApplicationService reportingApplicationService;
    private final AccountApplicationService accountApplicationService;
    private final WebCompanyContext companyContext;

    public BalanceSheetWebController(ReportingApplicationService reportingApplicationService,
                                     AccountApplicationService accountApplicationService,
                                     WebCompanyContext companyContext) {
        this.reportingApplicationService = reportingApplicationService;
        this.accountApplicationService = accountApplicationService;
        this.companyContext = companyContext;
    }

    @GetMapping
    public String balanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            Model model) {
        UUID companyId = companyContext.getCompanyId();
        model.addAttribute("pageTitle", "Balance Sheet");
        model.addAttribute("companyId", companyId);
        if (asOf != null) {
            BalanceSheetReport report = reportingApplicationService.getBalanceSheet(companyId, asOf);
            List<AccountResponse> accounts = accountApplicationService.listAccountsByCompany(companyId);
            Map<UUID, AccountResponse> accountMap = accounts.stream().collect(Collectors.toMap(AccountResponse::getId, a -> a));
            model.addAttribute("asOf", asOf);
            model.addAttribute("report", report);
            model.addAttribute("accountMap", accountMap);
            return "balance-sheet/result";
        }
        return "balance-sheet/form";
    }
}
