package com.jalaldeveloper.accountingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.jalaldeveloper.accountingsystem")
@EntityScan(basePackages = "com.jalaldeveloper.accountingsystem.dataaccess.entity")
@EnableJpaRepositories(basePackages = "com.jalaldeveloper.accountingsystem.dataaccess.repository")
public class AccountingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountingServiceApplication.class, args);
    }
}
