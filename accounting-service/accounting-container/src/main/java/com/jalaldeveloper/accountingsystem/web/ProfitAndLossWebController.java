package com.jalaldeveloper.accountingsystem.web;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.ProfitAndLossReport;
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
@RequestMapping("/web/profit-and-loss")
public class ProfitAndLossWebController {

    private final ReportingApplicationService reportingApplicationService;
    private final AccountApplicationService accountApplicationService;
    private final WebCompanyContext companyContext;

    public ProfitAndLossWebController(ReportingApplicationService reportingApplicationService,
                                      AccountApplicationService accountApplicationService,
                                      WebCompanyContext companyContext) {
        this.reportingApplicationService = reportingApplicationService;
        this.accountApplicationService = accountApplicationService;
        this.companyContext = companyContext;
    }

    @GetMapping
    public String profitAndLoss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {
        UUID companyId = companyContext.getCompanyId();
        model.addAttribute("pageTitle", "Profit and Loss");
        model.addAttribute("companyId", companyId);
        if (from != null && to != null) {
            ProfitAndLossReport report = reportingApplicationService.getProfitAndLoss(companyId, from, to);
            List<AccountResponse> accounts = accountApplicationService.listAccountsByCompany(companyId);
            Map<UUID, AccountResponse> accountMap = accounts.stream().collect(Collectors.toMap(AccountResponse::getId, a -> a));
            model.addAttribute("from", from);
            model.addAttribute("to", to);
            model.addAttribute("report", report);
            model.addAttribute("accountMap", accountMap);
            return "profit-and-loss/result";
        }
        return "profit-and-loss/form";
    }
}
