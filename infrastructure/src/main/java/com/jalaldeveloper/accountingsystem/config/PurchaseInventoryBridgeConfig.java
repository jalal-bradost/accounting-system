package com.jalaldeveloper.accountingsystem.config;

import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.PurchaseReceiveSyncPort;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.input.PurchaseApplicationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "false", matchIfMissing = true)
public class PurchaseInventoryBridgeConfig {

    @Bean
    public PurchaseReceiveSyncPort purchaseReceiveSyncPort(@Lazy PurchaseApplicationService purchaseApplicationService) {
        return purchaseApplicationService::syncPurchaseOrderLineQtyReceivedFromStockMoves;
    }
}
