package com.jalaldeveloper.accountingsystem.web;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CompanySettingsApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/web/company")
public class CompanySettingsWebController {

    private final CompanySettingsApplicationService companySettingsApplicationService;
    private final WebCompanyContext companyContext;

    public CompanySettingsWebController(CompanySettingsApplicationService companySettingsApplicationService,
                                        WebCompanyContext companyContext) {
        this.companySettingsApplicationService = companySettingsApplicationService;
        this.companyContext = companyContext;
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "Company Settings");
        model.addAttribute("companyId", companyContext.getCompanyId());
        return "company/settings";
    }

    @PostMapping("/settings")
    public String setLockDate(@RequestParam String periodLockDate, RedirectAttributes redirectAttributes) {
        UUID companyId = companyContext.getCompanyId();
        try {
            companySettingsApplicationService.setPeriodLockDate(companyId, LocalDate.parse(periodLockDate));
            redirectAttributes.addFlashAttribute("message", "Period lock date updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/company/settings";
    }
}
