package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.messaging;

import com.jalaldeveloper.accountingsystem.inventory.service.domain.event.StockPickingValidatedEvent;

public interface InventoryEventPublisher {

    void publishStockPickingValidated(StockPickingValidatedEvent event);
}
