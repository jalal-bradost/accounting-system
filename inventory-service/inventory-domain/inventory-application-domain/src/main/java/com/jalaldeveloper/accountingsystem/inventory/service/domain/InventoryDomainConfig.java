package com.jalaldeveloper.accountingsystem.inventory.service.domain;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.service.ValuationStrategyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the pure-domain {@link ValuationStrategyFactory} into the Spring context so the
 * application service can inject it.
 */
@Configuration
public class InventoryDomainConfig {

    @Bean
    public ValuationStrategyFactory valuationStrategyFactory() {
        return new ValuationStrategyFactory();
    }
}
