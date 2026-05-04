package com.jalaldeveloper.accountingsystem.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Placeholder for reconciliation UI. Full implementation would allow
 * selecting journal items and setting/clearing reconciliation ID.
 *
 * @deprecated The Thymeleaf {@code /web} UI is being replaced by the Next.js frontend in
 * {@code accounting-system-frontend}. Kept during the migration; will be removed once the
 * new client reaches feature parity. See {@code docs/MIGRATION.md}.
 */
@Deprecated(since = "0.2.0", forRemoval = true)
@Controller
@RequestMapping("/web/reconciliation")
public class ReconciliationWebController {

    private final WebCompanyContext companyContext;

    public ReconciliationWebController(WebCompanyContext companyContext) {
        this.companyContext = companyContext;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "Reconciliation");
        model.addAttribute("companyId", companyContext.getCompanyId());
        return "reconciliation/index";
    }
}
