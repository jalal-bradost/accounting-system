package com.bradox.delin.config;

import com.bradox.delin.inventory.service.domain.ports.output.SalesDeliverySyncPort;
import com.bradox.delin.sales.service.domain.ports.input.SalesApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class SalesInventoryBridgeConfig {

    @Bean
    public SalesDeliverySyncPort salesDeliverySyncPort(@Lazy SalesApplicationService salesApplicationService) {
        return salesApplicationService::afterOutgoingPickingValidated;
    }
}
