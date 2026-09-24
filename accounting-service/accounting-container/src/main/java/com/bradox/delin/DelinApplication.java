package com.bradox.delin;

import com.bradox.delin.config.AccountingCurrencyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.bradox.delin")
@EnableConfigurationProperties(AccountingCurrencyProperties.class)
@EntityScan(basePackages = {
        "com.bradox.delin.dataaccess.entity",
        "com.bradox.delin.platform.dataaccess.entity",
        "com.bradox.delin.contacts.dataaccess.entity",
        "com.bradox.delin.hr.dataaccess.entity",
        "com.bradox.delin.expense.dataaccess.entity",
        "com.bradox.delin.inventory.dataaccess.entity",
        "com.bradox.delin.purchase.dataaccess.entity",
        "com.bradox.delin.sales.dataaccess.entity",
        "com.bradox.delin.pos.dataaccess.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.bradox.delin.dataaccess.repository",
        "com.bradox.delin.platform.dataaccess.repository",
        "com.bradox.delin.contacts.dataaccess.repository",
        "com.bradox.delin.hr.dataaccess.repository",
        "com.bradox.delin.expense.dataaccess.repository",
        "com.bradox.delin.inventory.dataaccess.repository",
        "com.bradox.delin.purchase.dataaccess.repository",
        "com.bradox.delin.sales.dataaccess.repository",
        "com.bradox.delin.pos.dataaccess.repository"
})
public class DelinApplication {

    public static void main(String[] args) {
        SpringApplication.run(DelinApplication.class, args);
    }
}
