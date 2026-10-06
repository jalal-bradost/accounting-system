package com.bradox.erp.bootstrap;

import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ensures inventory defaults for the demo company. Idempotent — safe when
 * {@link DefaultCompanyChartDataSeeder} already ran the full {@link CompanyErpBootstrap}.
 */
@Component
@Order(2)
@ConditionalOnProperty(
        name = "inventory.seed.default-company",
        havingValue = "true",
        matchIfMissing = true)
public class DefaultInventorySeeder implements ApplicationRunner {

    private final CompanyErpBootstrap companyErpBootstrap;

    public DefaultInventorySeeder(CompanyErpBootstrap companyErpBootstrap) {
        this.companyErpBootstrap = companyErpBootstrap;
    }

    @Override
    public void run(ApplicationArguments args) {
        companyErpBootstrap.bootstrap(PlatformRbacSeeder.DEFAULT_COMPANY_ID);
    }
}
