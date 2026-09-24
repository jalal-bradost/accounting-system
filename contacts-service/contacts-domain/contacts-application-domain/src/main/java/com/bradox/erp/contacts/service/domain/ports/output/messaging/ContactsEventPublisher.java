package com.bradox.erp.contacts.service.domain.ports.output.messaging;

import com.bradox.erp.contacts.service.domain.event.PartnerUpdatedEvent;

public interface ContactsEventPublisher {

    void publishPartnerUpdated(PartnerUpdatedEvent event);
}
