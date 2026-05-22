package com.jalaldeveloper.accountingsystem.inventory.messaging;

import com.jalaldeveloper.accountingsystem.inventory.service.domain.event.StockPickingValidatedEvent;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.messaging.InventoryEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class InventoryRabbitEventPublisher implements InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventoryRabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public InventoryRabbitEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.exchange:accounting.system.events}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publishStockPickingValidated(StockPickingValidatedEvent event) {
        publishAfterCommit("inventory.stock-picking.validated", event);
    }

    private void publishAfterCommit(String routingKey, Object payload) {
        Runnable dispatch = () -> {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            } catch (RuntimeException ex) {
                log.warn("Inventory event publish failed for key {}", routingKey, ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }
}
