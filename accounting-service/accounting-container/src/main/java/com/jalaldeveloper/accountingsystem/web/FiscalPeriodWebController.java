package com.jalaldeveloper.accountingsystem.web;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.FiscalPeriodApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.FiscalPeriodRepository.FiscalPeriodInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/web/fiscal-periods")
public class FiscalPeriodWebController {

    private final FiscalPeriodApplicationService fiscalPeriodApplicationService;
    private final WebCompanyContext companyContext;

    public FiscalPeriodWebController(FiscalPeriodApplicationService fiscalPeriodApplicationService,
                                     WebCompanyContext companyContext) {
        this.fiscalPeriodApplicationService = fiscalPeriodApplicationService;
        this.companyContext = companyContext;
    }

    @GetMapping
    public String list(Model model) {
        UUID companyId = companyContext.getCompanyId();
        List<FiscalPeriodInfo> periods = fiscalPeriodApplicationService.listPeriods(companyId);
        model.addAttribute("pageTitle", "Fiscal Periods");
        model.addAttribute("companyId", companyId);
        model.addAttribute("periods", periods);
        return "fiscal-periods/list";
    }

    @PostMapping
    public String create(@RequestParam String startDate,
                         @RequestParam String endDate,
                         @RequestParam(defaultValue = "true") boolean open,
                         RedirectAttributes redirectAttributes) {
        UUID companyId = companyContext.getCompanyId();
        try {
            fiscalPeriodApplicationService.createPeriod(
                    companyId,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    open
            );
            redirectAttributes.addFlashAttribute("message", "Fiscal period created.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/fiscal-periods";
    }
}
