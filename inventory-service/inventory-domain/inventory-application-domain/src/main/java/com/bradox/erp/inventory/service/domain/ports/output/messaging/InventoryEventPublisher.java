package com.bradox.erp.inventory.service.domain.ports.output.messaging;

import com.bradox.erp.inventory.service.domain.event.StockPickingValidatedEvent;

public interface InventoryEventPublisher {

    void publishStockPickingValidated(StockPickingValidatedEvent event);
}
