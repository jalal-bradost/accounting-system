package com.jalaldeveloper.accountingsystem.web;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateAccountCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateAccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * @deprecated The Thymeleaf {@code /web} UI is being replaced by the Next.js frontend in
 * {@code accounting-system-frontend}. Kept during the migration; will be removed once the
 * new client reaches feature parity. See {@code docs/MIGRATION.md}.
 */
@Deprecated(since = "0.2.0", forRemoval = true)
@Controller
@RequestMapping("/web/accounts")
public class AccountWebController {

    private final AccountApplicationService accountApplicationService;
    private final WebCompanyContext companyContext;

    public AccountWebController(AccountApplicationService accountApplicationService,
                                WebCompanyContext companyContext) {
        this.accountApplicationService = accountApplicationService;
        this.companyContext = companyContext;
    }

    @GetMapping
    public String list(Model model) {
        UUID companyId = companyContext.getCompanyId();
        List<AccountResponse> accounts = accountApplicationService.listAccountsByCompany(companyId);
        model.addAttribute("pageTitle", "Chart of Accounts");
        model.addAttribute("companyId", companyId);
        model.addAttribute("accounts", accounts);
        return "accounts/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("pageTitle", "New Account");
        model.addAttribute("companyId", companyContext.getCompanyId());
        model.addAttribute("accountTypes", AccountType.values());
        if (!model.containsAttribute("createAccountForm")) {
            model.addAttribute("createAccountForm", new CreateAccountForm());
        }
        return "accounts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("createAccountForm") CreateAccountForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.createAccountForm", bindingResult);
            redirectAttributes.addFlashAttribute("createAccountForm", form);
            return "redirect:/web/accounts/new";
        }
        UUID companyId = companyContext.getCompanyId();
        CreateAccountCommand command = new CreateAccountCommand(
                companyId,
                form.getCode(),
                form.getName(),
                AccountType.valueOf(form.getAccountType()),
                form.isActive()
        );
        CreateAccountResponse response = accountApplicationService.createAccount(command);
        redirectAttributes.addFlashAttribute("message", "Account created: " + response.getAccountId());
        return "redirect:/web/accounts";
    }

    /**
     * Form backing object for create account (mutable for binding).
     */
    public static class CreateAccountForm {
        @NotBlank(message = "Code is required")
        private String code;
        @NotBlank(message = "Name is required")
        private String name;
        private String accountType = "ASSET";
        private boolean active = true;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
