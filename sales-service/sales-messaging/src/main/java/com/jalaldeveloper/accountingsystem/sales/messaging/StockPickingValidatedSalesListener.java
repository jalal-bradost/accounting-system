package com.jalaldeveloper.accountingsystem.sales.messaging;

import com.jalaldeveloper.accountingsystem.inventory.service.domain.event.StockPickingValidatedEvent;
import com.jalaldeveloper.accountingsystem.messaging.consumer.IntegrationEventDedupService;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.input.SalesApplicationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
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
            salesApplicationService.afterOutgoingPickingValidated(event.salesOrderId());
        }
    }
}
