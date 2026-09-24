package com.bradox.delin.sales.messaging;

import com.bradox.delin.inventory.service.domain.event.StockPickingValidatedEvent;
import com.bradox.delin.messaging.consumer.IntegrationEventDedupService;
import com.bradox.delin.sales.service.domain.ports.input.SalesApplicationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class StockPickingValidatedSalesListener {

    private final SalesApplicationService salesApplicationService;
    private final IntegrationEventDedupService dedupService;

    public StockPickingValidatedSalesListener(SalesApplicationService salesApplicationService,
                                              IntegrationEventDedupService dedupService) {
        this.salesApplicationService = salesApplicationService;
        this.dedupService = dedupService;
    }

    @RabbitListener(queues = "sales.inventory-sync.q")
    public void onStockPickingValidated(StockPickingValidatedEvent event) {
        if (!dedupService.shouldProcess(event.eventId(), "inventory.stock-picking.validated", "sales-sync-consumer")) {
            return;
        }
        if (event.salesOrderId() != null) {
            salesApplicationService.afterOutgoingPickingValidated(event.salesOrderId(), event.pickingId());
        }
    }
}
