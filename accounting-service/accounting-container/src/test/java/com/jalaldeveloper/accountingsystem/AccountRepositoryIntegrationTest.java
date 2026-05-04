package com.jalaldeveloper.accountingsystem;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Account;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AccountRepositoryIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void save_andFindById_returnsSavedAccount() {
        UUID companyId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder()
                .id(new AccountId(accountId))
                .companyId(new CompanyId(companyId))
                .code("1000")
                .name("Cash")
                .accountType(AccountType.BANK_AND_CASH)
                .active(true)
                .build();

        Account saved = accountRepository.save(account);
        assertThat(saved).isNotNull();
        assertThat(saved.getId().getId()).isEqualTo(accountId);

        Optional<Account> found = accountRepository.findById(new AccountId(accountId));
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("1000");
        assertThat(found.get().getName()).isEqualTo("Cash");
    }

    @Test
    void findByCompanyId_returnsAccountsForCompany() {
        UUID companyId = UUID.randomUUID();
        accountRepository.save(Account.builder()
                .id(new AccountId(UUID.randomUUID()))
                .companyId(new CompanyId(companyId))
                .code("2000")
                .name("Receivables")
                .accountType(AccountType.RECEIVABLE)
                .active(true)
                .build());

        List<Account> list = accountRepository.findByCompanyId(new CompanyId(companyId));
        assertThat(list).isNotEmpty();
        assertThat(list.stream().anyMatch(a -> "2000".equals(a.getCode()))).isTrue();
    }
}
