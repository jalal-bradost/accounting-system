package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateAccountCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateAccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/accounts", produces = "application/json")
public class AccountController {

    private final AccountApplicationService accountApplicationService;

    public AccountController(AccountApplicationService accountApplicationService) {
        this.accountApplicationService = accountApplicationService;
    }

    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(@Valid @RequestBody CreateAccountCommand command) {
        CreateAccountResponse response = accountApplicationService.createAccount(command);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(accountApplicationService.getAccount(id));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts(@RequestParam UUID companyId) {
        return ResponseEntity.ok(accountApplicationService.listAccountsByCompany(companyId));
    }
}
