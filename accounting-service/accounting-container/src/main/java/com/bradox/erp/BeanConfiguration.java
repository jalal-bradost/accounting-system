package com.bradox.erp;

import com.bradox.erp.domain.core.AccountingDomainService;
import com.bradox.erp.domain.core.AccountingDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public AccountingDomainService accountingDomainService() {
        return new AccountingDomainServiceImpl();
    }
}
