package com.jalaldeveloper.accountingsystem.purchase.messaging;

import com.jalaldeveloper.accountingsystem.purchase.service.domain.event.VendorBillPostedEvent;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.event.VendorPaymentRegisteredEvent;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.messaging.PurchaseEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class PurchaseRabbitEventPublisher implements PurchaseEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PurchaseRabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public PurchaseRabbitEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.exchange:accounting.system.events}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publishVendorBillPosted(VendorBillPostedEvent event) {
        publishAfterCommit("purchase.vendor-bill.posted", event);
    }

    @Override
    public void publishVendorPaymentRegistered(VendorPaymentRegisteredEvent event) {
        publishAfterCommit("purchase.vendor-payment.registered", event);
    }

    private void publishAfterCommit(String routingKey, Object payload) {
        Runnable dispatch = () -> {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            } catch (RuntimeException ex) {
                log.warn("Purchase event publish failed for key {}", routingKey, ex);
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
