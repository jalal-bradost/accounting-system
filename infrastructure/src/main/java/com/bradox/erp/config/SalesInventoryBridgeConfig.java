package com.bradox.erp.config;

import com.bradox.erp.inventory.service.domain.ports.output.SalesDeliverySyncPort;
import com.bradox.erp.sales.service.domain.ports.input.SalesApplicationService;
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
