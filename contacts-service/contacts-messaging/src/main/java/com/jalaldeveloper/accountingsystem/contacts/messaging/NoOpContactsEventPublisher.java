package com.jalaldeveloper.accountingsystem.contacts.messaging;

import com.jalaldeveloper.accountingsystem.contacts.service.domain.event.PartnerUpdatedEvent;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.messaging.ContactsEventPublisher;
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
