package com.jalaldeveloper.accountingsystem.contacts.messaging;

import com.jalaldeveloper.accountingsystem.contacts.service.domain.event.PartnerUpdatedEvent;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.messaging.ContactsEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ContactsRabbitEventPublisher implements ContactsEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ContactsRabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public ContactsRabbitEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.exchange:accounting.system.events}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publishPartnerUpdated(PartnerUpdatedEvent event) {
        publishAfterCommit("contacts.partner.updated", event);
    }

    private void publishAfterCommit(String routingKey, Object payload) {
        Runnable dispatch = () -> {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            } catch (RuntimeException ex) {
                log.warn("Contacts event publish failed for key {}", routingKey, ex);
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
