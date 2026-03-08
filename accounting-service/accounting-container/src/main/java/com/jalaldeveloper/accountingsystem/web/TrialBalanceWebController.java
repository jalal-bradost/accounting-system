package com.jalaldeveloper.accountingsystem.web;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
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

@Controller
@RequestMapping("/web/trial-balance")
public class TrialBalanceWebController {

    private final ReportingApplicationService reportingApplicationService;
    private final AccountApplicationService accountApplicationService;
    private final WebCompanyContext companyContext;

    public TrialBalanceWebController(ReportingApplicationService reportingApplicationService,
                                     AccountApplicationService accountApplicationService,
                                     WebCompanyContext companyContext) {
        this.reportingApplicationService = reportingApplicationService;
        this.accountApplicationService = accountApplicationService;
        this.companyContext = companyContext;
    }

    @GetMapping
    public String trialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {
        UUID companyId = companyContext.getCompanyId();
        model.addAttribute("pageTitle", "Trial Balance");
        model.addAttribute("companyId", companyId);
        if (from != null && to != null) {
            List<AccountBalanceRepository.AccountBalanceLine> lines = reportingApplicationService.getTrialBalance(companyId, from, to);
            List<AccountResponse> accounts = accountApplicationService.listAccountsByCompany(companyId);
            Map<UUID, AccountResponse> accountMap = accounts.stream().collect(Collectors.toMap(AccountResponse::getId, a -> a));
            model.addAttribute("from", from);
            model.addAttribute("to", to);
            model.addAttribute("lines", lines);
            model.addAttribute("accountMap", accountMap);
            return "trial-balance/result";
        }
        return "trial-balance/form";
    }
}
