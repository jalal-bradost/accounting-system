package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateAccountCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateAccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.AccountApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
class AccountApplicationServiceImpl implements AccountApplicationService {

    private final CreateAccountCommandHandler createAccountCommandHandler;
    private final AccountRepository accountRepository;
    private final AccountingDataMapper mapper;

    AccountApplicationServiceImpl(CreateAccountCommandHandler createAccountCommandHandler,
                                  AccountRepository accountRepository,
                                  AccountingDataMapper mapper) {
        this.createAccountCommandHandler = createAccountCommandHandler;
        this.accountRepository = accountRepository;
        this.mapper = mapper;
    }

    @Override
    public CreateAccountResponse createAccount(CreateAccountCommand command) {
        return createAccountCommandHandler.createAccount(command);
    }

    @Override
    public AccountResponse getAccount(UUID accountId) {
        return accountRepository.findById(new AccountId(accountId))
                .map(mapper::accountToAccountResponse)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }

    @Override
    public List<AccountResponse> listAccountsByCompany(UUID companyId) {
        return accountRepository.findByCompanyId(new CompanyId(companyId)).stream()
                .map(mapper::accountToAccountResponse)
                .collect(Collectors.toList());
    }
}
