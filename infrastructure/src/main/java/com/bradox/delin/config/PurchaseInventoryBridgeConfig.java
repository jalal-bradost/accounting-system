package com.bradox.delin.config;

import com.bradox.delin.inventory.service.domain.ports.output.PurchaseReceiveSyncPort;
import com.bradox.delin.purchase.service.domain.ports.input.PurchaseApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class PurchaseInventoryBridgeConfig {

    @Bean
    public PurchaseReceiveSyncPort purchaseReceiveSyncPort(@Lazy PurchaseApplicationService purchaseApplicationService) {
        return purchaseApplicationService::syncPurchaseOrderLineQtyReceivedFromStockMoves;
    }
}
