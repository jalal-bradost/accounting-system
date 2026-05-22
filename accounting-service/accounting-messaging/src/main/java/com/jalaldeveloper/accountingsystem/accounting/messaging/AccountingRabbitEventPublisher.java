package com.jalaldeveloper.accountingsystem.accounting.messaging;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.event.CustomerInvoicePostedEvent;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.messaging.AccountingEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AccountingRabbitEventPublisher implements AccountingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AccountingRabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public AccountingRabbitEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.exchange:accounting.system.events}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publishCustomerInvoicePosted(CustomerInvoicePostedEvent event) {
        publishAfterCommit("accounting.customer-invoice.posted", event);
    }

    private void publishAfterCommit(String routingKey, Object payload) {
        Runnable dispatch = () -> {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            } catch (RuntimeException ex) {
                log.warn("Accounting event publish failed for key {}", routingKey, ex);
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
