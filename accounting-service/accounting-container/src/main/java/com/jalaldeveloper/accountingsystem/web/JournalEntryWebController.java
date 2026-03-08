package com.jalaldeveloper.accountingsystem.web;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalItemCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/web/journal-entries")
public class JournalEntryWebController {

    private final JournalEntryApplicationService journalEntryApplicationService;
    private final AccountApplicationService accountApplicationService;
    private final JournalApplicationService journalApplicationService;
    private final WebCompanyContext companyContext;

    public JournalEntryWebController(JournalEntryApplicationService journalEntryApplicationService,
                                     AccountApplicationService accountApplicationService,
                                     JournalApplicationService journalApplicationService,
                                     WebCompanyContext companyContext) {
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.accountApplicationService = accountApplicationService;
        this.journalApplicationService = journalApplicationService;
        this.companyContext = companyContext;
    }

    @GetMapping
    public String list(Model model) {
        UUID companyId = companyContext.getCompanyId();
        List<JournalEntryResponse> entries = journalEntryApplicationService.listJournalEntriesByCompany(companyId);
        model.addAttribute("pageTitle", "Journal Entries");
        model.addAttribute("companyId", companyId);
        model.addAttribute("entries", entries);
        return "journal-entries/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        UUID companyId = companyContext.getCompanyId();
        model.addAttribute("pageTitle", "New Journal Entry");
        model.addAttribute("companyId", companyId);
        model.addAttribute("accounts", accountApplicationService.listAccountsByCompany(companyId));
        model.addAttribute("journals", journalApplicationService.listJournalsByCompany(companyId));
        if (!model.containsAttribute("createEntryForm")) {
            CreateJournalEntryForm form = new CreateJournalEntryForm();
            form.setDate(LocalDate.now().toString());
            form.setLines(new ArrayList<>(List.of(
                    new JournalEntryLineForm(), new JournalEntryLineForm(),
                    new JournalEntryLineForm(), new JournalEntryLineForm(), new JournalEntryLineForm()
            )));
            model.addAttribute("createEntryForm", form);
        }
        return "journal-entries/form";
    }

    @PostMapping
    public String create(@ModelAttribute("createEntryForm") CreateJournalEntryForm form,
                        RedirectAttributes redirectAttributes) {
        UUID companyId = companyContext.getCompanyId();
        List<JournalItemCommand> items = new ArrayList<>();
        for (JournalEntryLineForm line : form.getLines()) {
            if (line.getAccountId() != null && !line.getAccountId().isBlank()) {
                BigDecimal debit = toDecimal(line.getDebit());
                BigDecimal credit = toDecimal(line.getCredit());
                if (debit.compareTo(BigDecimal.ZERO) > 0 || credit.compareTo(BigDecimal.ZERO) > 0) {
                    items.add(new JournalItemCommand(
                            UUID.fromString(line.getAccountId()),
                            line.getLabel(),
                            debit,
                            credit,
                            null,
                            null
                    ));
                }
            }
        }
        CreateJournalEntryCommand command = new CreateJournalEntryCommand(
                companyId,
                UUID.fromString(form.getJournalId()),
                form.getSequenceNumber() != null ? form.getSequenceNumber() : "",
                LocalDate.parse(form.getDate()),
                form.getCurrencyCode(),
                items
        );
        try {
            CreateJournalEntryResponse response = journalEntryApplicationService.createJournalEntry(command);
            redirectAttributes.addFlashAttribute("message", "Journal entry created: " + response.getMessage());
            return "redirect:/web/journal-entries/" + response.getJournalEntryId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("createEntryForm", form);
            return "redirect:/web/journal-entries/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        JournalEntryResponse entry = journalEntryApplicationService.getJournalEntry(id);
        model.addAttribute("pageTitle", "Journal Entry " + entry.getSequenceNumber());
        model.addAttribute("companyId", entry.getCompanyId());
        model.addAttribute("entry", entry);
        return "journal-entries/detail";
    }

    @PostMapping("/{id}/post")
    public String post(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            journalEntryApplicationService.postJournalEntry(id);
            redirectAttributes.addFlashAttribute("message", "Entry posted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/journal-entries/" + id;
    }

    @PostMapping("/{id}/reverse")
    public String reverse(@PathVariable UUID id,
                          @org.springframework.web.bind.annotation.RequestParam(required = false) String reason,
                          RedirectAttributes redirectAttributes) {
        try {
            journalEntryApplicationService.reverseJournalEntry(
                    new ReverseJournalEntryCommand(id, reason != null && !reason.isBlank() ? reason : "Reversal from dashboard"));
            redirectAttributes.addFlashAttribute("message", "Entry reversed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/journal-entries/" + id;
    }

    private static BigDecimal toDecimal(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static class CreateJournalEntryForm {
        private String journalId;
        private String date;
        private String sequenceNumber;
        private String currencyCode;
        private List<JournalEntryLineForm> lines = new ArrayList<>();

        public String getJournalId() { return journalId; }
        public void setJournalId(String journalId) { this.journalId = journalId; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getSequenceNumber() { return sequenceNumber; }
        public void setSequenceNumber(String sequenceNumber) { this.sequenceNumber = sequenceNumber; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
        public List<JournalEntryLineForm> getLines() { return lines; }
        public void setLines(List<JournalEntryLineForm> lines) { this.lines = lines; }
    }

    public static class JournalEntryLineForm {
        private String accountId;
        private String label;
        private String debit;
        private String credit;

        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getDebit() { return debit; }
        public void setDebit(String debit) { this.debit = debit; }
        public String getCredit() { return credit; }
        public void setCredit(String credit) { this.credit = credit; }
    }
}
