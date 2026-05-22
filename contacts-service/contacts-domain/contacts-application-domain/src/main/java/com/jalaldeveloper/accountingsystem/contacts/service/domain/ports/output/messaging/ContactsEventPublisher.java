package com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.messaging;

import com.jalaldeveloper.accountingsystem.contacts.service.domain.event.PartnerUpdatedEvent;

public interface ContactsEventPublisher {

    void publishPartnerUpdated(PartnerUpdatedEvent event);
}
