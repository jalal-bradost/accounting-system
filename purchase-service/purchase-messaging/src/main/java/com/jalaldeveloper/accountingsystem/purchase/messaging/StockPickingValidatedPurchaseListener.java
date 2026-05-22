package com.jalaldeveloper.accountingsystem.purchase.messaging;

import com.jalaldeveloper.accountingsystem.inventory.service.domain.event.StockPickingValidatedEvent;
import com.jalaldeveloper.accountingsystem.messaging.consumer.IntegrationEventDedupService;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.input.PurchaseApplicationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StockPickingValidatedPurchaseListener {

    private final PurchaseApplicationService purchaseApplicationService;
    private final IntegrationEventDedupService dedupService;

    public StockPickingValidatedPurchaseListener(PurchaseApplicationService purchaseApplicationService,
                                                 IntegrationEventDedupService dedupService) {
        this.purchaseApplicationService = purchaseApplicationService;
        this.dedupService = dedupService;
    }

    @RabbitListener(queues = "purchase.inventory-sync.q")
    public void onStockPickingValidated(StockPickingValidatedEvent event) {
        if (!dedupService.shouldProcess(event.eventId(), "inventory.stock-picking.validated", "purchase-sync-consumer")) {
            return;
        }
        if (event.purchaseOrderId() != null) {
            purchaseApplicationService.syncPurchaseOrderLineQtyReceivedFromStockMoves(event.purchaseOrderId());
        }
    }
}
