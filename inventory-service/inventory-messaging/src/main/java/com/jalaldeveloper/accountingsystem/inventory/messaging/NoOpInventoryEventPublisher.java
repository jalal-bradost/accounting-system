package com.jalaldeveloper.accountingsystem.inventory.messaging;

import com.jalaldeveloper.accountingsystem.inventory.service.domain.event.StockPickingValidatedEvent;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.messaging.InventoryEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpInventoryEventPublisher implements InventoryEventPublisher {

    @Override
    public void publishStockPickingValidated(StockPickingValidatedEvent event) {
        // Messaging disabled — inventory sync is not published.
    }
}
