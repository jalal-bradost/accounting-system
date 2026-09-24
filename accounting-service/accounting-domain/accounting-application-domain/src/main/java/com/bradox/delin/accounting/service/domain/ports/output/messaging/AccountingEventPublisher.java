package com.bradox.delin.accounting.service.domain.ports.output.messaging;

import com.bradox.delin.accounting.service.domain.event.CustomerInvoicePostedEvent;

public interface AccountingEventPublisher {

    void publishCustomerInvoicePosted(CustomerInvoicePostedEvent event);
}
