package com.bradox.delin.accounting.messaging;

import com.bradox.delin.accounting.service.domain.event.CustomerInvoicePostedEvent;
import com.bradox.delin.accounting.service.domain.ports.output.messaging.AccountingEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAccountingEventPublisher implements AccountingEventPublisher {

    @Override
    public void publishCustomerInvoicePosted(CustomerInvoicePostedEvent event) {
        // Messaging disabled — accounting events are not published.
    }
}
