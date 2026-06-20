package com.jalaldeveloper.accountingsystem;

import com.jalaldeveloper.accountingsystem.config.AccountingCurrencyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.jalaldeveloper.accountingsystem")
@EnableConfigurationProperties(AccountingCurrencyProperties.class)
@EntityScan(basePackages = {
        "com.jalaldeveloper.accountingsystem.dataaccess.entity",
        "com.jalaldeveloper.accountingsystem.platform.dataaccess.entity",
        "com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity",
        "com.jalaldeveloper.accountingsystem.hr.dataaccess.entity",
        "com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity",
        "com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity",
        "com.jalaldeveloper.accountingsystem.sales.dataaccess.entity",
        "com.jalaldeveloper.accountingsystem.pos.dataaccess.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.jalaldeveloper.accountingsystem.dataaccess.repository",
        "com.jalaldeveloper.accountingsystem.platform.dataaccess.repository",
        "com.jalaldeveloper.accountingsystem.contacts.dataaccess.repository",
        "com.jalaldeveloper.accountingsystem.hr.dataaccess.repository",
        "com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository",
        "com.jalaldeveloper.accountingsystem.purchase.dataaccess.repository",
        "com.jalaldeveloper.accountingsystem.sales.dataaccess.repository",
        "com.jalaldeveloper.accountingsystem.pos.dataaccess.repository"
})
public class AccountingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountingServiceApplication.class, args);
    }
}
