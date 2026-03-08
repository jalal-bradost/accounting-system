package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateAccountCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateAccountResponse;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

public interface AccountApplicationService {

    CreateAccountResponse createAccount(@Valid CreateAccountCommand command);

    AccountResponse getAccount(UUID accountId);

    List<AccountResponse> listAccountsByCompany(UUID companyId);
}
