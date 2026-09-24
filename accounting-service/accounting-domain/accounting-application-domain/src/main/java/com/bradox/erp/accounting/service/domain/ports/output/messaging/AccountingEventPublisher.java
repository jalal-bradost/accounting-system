package com.bradox.erp.accounting.service.domain.ports.output.messaging;

import com.bradox.erp.accounting.service.domain.event.CustomerInvoicePostedEvent;

public interface AccountingEventPublisher {

    void publishCustomerInvoicePosted(CustomerInvoicePostedEvent event);
}
