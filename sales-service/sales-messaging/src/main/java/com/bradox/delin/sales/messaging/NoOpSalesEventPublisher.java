package com.bradox.delin.sales.messaging;

import com.bradox.delin.sales.service.domain.event.SalesOrderConfirmedEvent;
import com.bradox.delin.sales.service.domain.ports.output.messaging.SalesEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSalesEventPublisher implements SalesEventPublisher {

    @Override
    public void publishSalesOrderConfirmed(SalesOrderConfirmedEvent event) {
        // Messaging disabled — sales events are not published.
    }
}
