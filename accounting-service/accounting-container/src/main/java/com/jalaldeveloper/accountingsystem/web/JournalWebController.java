package com.jalaldeveloper.accountingsystem.web;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalApplicationService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
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
@RequestMapping("/web/journals")
public class JournalWebController {

    private final JournalApplicationService journalApplicationService;
    private final WebCompanyContext companyContext;

    public JournalWebController(JournalApplicationService journalApplicationService,
                               WebCompanyContext companyContext) {
        this.journalApplicationService = journalApplicationService;
        this.companyContext = companyContext;
    }

    @GetMapping
    public String list(Model model) {
        UUID companyId = companyContext.getCompanyId();
        List<JournalResponse> journals = journalApplicationService.listJournalsByCompany(companyId);
        model.addAttribute("pageTitle", "Journals");
        model.addAttribute("companyId", companyId);
        model.addAttribute("journals", journals);
        return "journals/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("pageTitle", "New Journal");
        model.addAttribute("companyId", companyContext.getCompanyId());
        model.addAttribute("journalTypes", JournalType.values());
        if (!model.containsAttribute("createJournalForm")) {
            model.addAttribute("createJournalForm", new CreateJournalForm());
        }
        return "journals/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("createJournalForm") CreateJournalForm form,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.createJournalForm", bindingResult);
            redirectAttributes.addFlashAttribute("createJournalForm", form);
            return "redirect:/web/journals/new";
        }
        UUID companyId = companyContext.getCompanyId();
        CreateJournalCommand command = new CreateJournalCommand(
                companyId,
                form.getCode(),
                form.getName(),
                JournalType.valueOf(form.getJournalType())
        );
        CreateJournalResponse response = journalApplicationService.createJournal(command);
        redirectAttributes.addFlashAttribute("message", "Journal created: " + response.getJournalId());
        return "redirect:/web/journals";
    }

    public static class CreateJournalForm {
        @NotBlank(message = "Code is required")
        private String code;
        @NotBlank(message = "Name is required")
        private String name;
        private String journalType = "MISC";

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getJournalType() { return journalType; }
        public void setJournalType(String journalType) { this.journalType = journalType; }
    }
}
