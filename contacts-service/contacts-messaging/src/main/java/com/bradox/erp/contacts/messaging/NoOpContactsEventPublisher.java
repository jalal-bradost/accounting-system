package com.bradox.erp.contacts.messaging;

import com.bradox.erp.contacts.service.domain.event.PartnerUpdatedEvent;
import com.bradox.erp.contacts.service.domain.ports.output.messaging.ContactsEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpContactsEventPublisher implements ContactsEventPublisher {

    @Override
    public void publishPartnerUpdated(PartnerUpdatedEvent event) {
        // Messaging disabled — partner events are not published.
    }
}
