package com.bradox.erp;

import com.bradox.erp.config.AccountingCurrencyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.bradox.erp")
@EnableConfigurationProperties(AccountingCurrencyProperties.class)
@EnableScheduling
@EntityScan(basePackages = {
        "com.bradox.erp.dataaccess.entity",
        "com.bradox.erp.platform.dataaccess.entity",
        "com.bradox.erp.contacts.dataaccess.entity",
        "com.bradox.erp.hr.dataaccess.entity",
        "com.bradox.erp.expense.dataaccess.entity",
        "com.bradox.erp.inventory.dataaccess.entity",
        "com.bradox.erp.purchase.dataaccess.entity",
        "com.bradox.erp.sales.dataaccess.entity",
        "com.bradox.erp.pos.dataaccess.entity",
        "com.bradox.erp.assistant.dataaccess.entity",
        "com.bradox.erp.documents.dataaccess.entity",
        "com.bradox.erp.timesheet.dataaccess.entity",
        "com.bradox.erp.sign.dataaccess.entity",
        "com.bradox.erp.repair.dataaccess.entity",
        "com.bradox.erp.project.dataaccess.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.bradox.erp.dataaccess.repository",
        "com.bradox.erp.platform.dataaccess.repository",
        "com.bradox.erp.contacts.dataaccess.repository",
        "com.bradox.erp.hr.dataaccess.repository",
        "com.bradox.erp.expense.dataaccess.repository",
        "com.bradox.erp.inventory.dataaccess.repository",
        "com.bradox.erp.purchase.dataaccess.repository",
        "com.bradox.erp.sales.dataaccess.repository",
        "com.bradox.erp.pos.dataaccess.repository",
        "com.bradox.erp.assistant.dataaccess.repository",
        "com.bradox.erp.documents.dataaccess.repository",
        "com.bradox.erp.timesheet.dataaccess.repository",
        "com.bradox.erp.sign.dataaccess.repository",
        "com.bradox.erp.repair.dataaccess.repository",
        "com.bradox.erp.project.dataaccess.repository"
})
public class DelinApplication {

    public static void main(String[] args) {
        SpringApplication.run(DelinApplication.class, args);
    }
}
