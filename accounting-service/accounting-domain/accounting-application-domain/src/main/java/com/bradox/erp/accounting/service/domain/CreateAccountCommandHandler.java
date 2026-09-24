package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.accounting.service.domain.create.CreateAccountCommand;
import com.bradox.erp.accounting.service.domain.create.CreateAccountResponse;
import com.bradox.erp.accounting.service.domain.mapper.AccountingDataMapper;
import com.bradox.erp.accounting.service.domain.ports.output.repository.AccountRepository;
import com.bradox.erp.domain.core.entity.Account;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class CreateAccountCommandHandler {

    private final AccountRepository accountRepository;
    private final AccountingDataMapper mapper;

    CreateAccountCommandHandler(AccountRepository accountRepository, AccountingDataMapper mapper) {
        this.accountRepository = accountRepository;
        this.mapper = mapper;
    }

    @Transactional
    CreateAccountResponse createAccount(CreateAccountCommand command) {
        if (accountRepository.existsByCompanyIdAndCode(new CompanyId(command.getCompanyId()), command.getCode())) {
            throw new IllegalArgumentException("Account already exists with code: " + command.getCode());
        }
        UUID id = UUID.randomUUID();
        Account account = mapper.createAccountCommandToAccount(command, id);
        Account saved = accountRepository.save(account);
        return mapper.accountToCreateAccountResponse(saved, "Account created successfully.");
    }
}
