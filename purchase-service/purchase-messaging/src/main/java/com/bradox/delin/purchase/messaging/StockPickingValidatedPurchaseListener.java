package com.bradox.delin.purchase.messaging;

import com.bradox.delin.inventory.service.domain.event.StockPickingValidatedEvent;
import com.bradox.delin.messaging.consumer.IntegrationEventDedupService;
import com.bradox.delin.purchase.service.domain.ports.input.PurchaseApplicationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
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
            purchaseApplicationService.syncPurchaseOrderLineQtyReceivedFromStockMoves(
                    event.purchaseOrderId(), event.pickingId());
        }
    }
}
