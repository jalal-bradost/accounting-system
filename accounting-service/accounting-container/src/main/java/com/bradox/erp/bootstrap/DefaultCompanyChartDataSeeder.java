package com.bradox.erp.bootstrap;

import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds default chart of accounts and journals for the demo company when missing.
 * Logic lives in {@link CompanyErpBootstrap}.
 */
@Component
@Order(0)
@ConditionalOnProperty(
        name = "accounting.seed.default-company-chart",
        havingValue = "true",
        matchIfMissing = true)
public class DefaultCompanyChartDataSeeder implements ApplicationRunner {

    private final CompanyErpBootstrap companyErpBootstrap;

    public DefaultCompanyChartDataSeeder(CompanyErpBootstrap companyErpBootstrap) {
        this.companyErpBootstrap = companyErpBootstrap;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Full ERP bootstrap (chart + inventory). Inventory-only seeder is a no-op wrapper.
        companyErpBootstrap.bootstrap(PlatformRbacSeeder.DEFAULT_COMPANY_ID);
    }
}
