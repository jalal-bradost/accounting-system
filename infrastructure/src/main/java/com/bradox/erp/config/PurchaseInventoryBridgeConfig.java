package com.bradox.erp.config;

import com.bradox.erp.inventory.service.domain.ports.output.PurchaseReceiveSyncPort;
import com.bradox.erp.purchase.service.domain.ports.input.PurchaseApplicationService;
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
