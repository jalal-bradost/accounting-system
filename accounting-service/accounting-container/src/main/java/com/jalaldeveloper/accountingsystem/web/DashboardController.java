package com.jalaldeveloper.accountingsystem.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Serves the dashboard home and redirects root to /web.
 */
@Controller
@RequestMapping("/web")
public class DashboardController {

    /**
     * Default company ID for demo when no session company is set.
     * Can be overridden via application property later.
     */
    public static final UUID DEFAULT_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @GetMapping({"", "/"})
    public String home(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("companyId", DEFAULT_COMPANY_ID);
        return "dashboard/home";
    }
}
