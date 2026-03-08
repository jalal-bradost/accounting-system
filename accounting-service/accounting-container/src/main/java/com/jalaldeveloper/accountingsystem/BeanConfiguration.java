package com.jalaldeveloper.accountingsystem;

import com.jalaldeveloper.accountingsystem.domain.core.AccountingDomainService;
import com.jalaldeveloper.accountingsystem.domain.core.AccountingDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public AccountingDomainService accountingDomainService() {
        return new AccountingDomainServiceImpl();
    }
}
