package com.jalaldeveloper.accountingsystem.sales.messaging;

import com.jalaldeveloper.accountingsystem.sales.service.domain.event.SalesOrderConfirmedEvent;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.messaging.SalesEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SalesRabbitEventPublisher implements SalesEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SalesRabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public SalesRabbitEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.exchange:accounting.system.events}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publishSalesOrderConfirmed(SalesOrderConfirmedEvent event) {
        publishAfterCommit("sales.order.confirmed", event);
    }

    private void publishAfterCommit(String routingKey, Object payload) {
        Runnable dispatch = () -> {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            } catch (RuntimeException ex) {
                log.warn("Sales event publish failed for key {}", routingKey, ex);
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
