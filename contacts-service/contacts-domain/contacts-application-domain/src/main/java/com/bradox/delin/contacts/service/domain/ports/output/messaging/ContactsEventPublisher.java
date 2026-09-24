package com.bradox.delin.contacts.service.domain.ports.output.messaging;

import com.bradox.delin.contacts.service.domain.event.PartnerUpdatedEvent;

public interface ContactsEventPublisher {

    void publishPartnerUpdated(PartnerUpdatedEvent event);
}
