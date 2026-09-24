package com.bradox.delin.inventory.service.domain.ports.output.messaging;

import com.bradox.delin.inventory.service.domain.event.StockPickingValidatedEvent;

public interface InventoryEventPublisher {

    void publishStockPickingValidated(StockPickingValidatedEvent event);
}
