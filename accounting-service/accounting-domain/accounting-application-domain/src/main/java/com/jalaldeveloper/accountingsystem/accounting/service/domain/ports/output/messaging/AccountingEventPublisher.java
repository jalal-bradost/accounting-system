package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.messaging;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.event.CustomerInvoicePostedEvent;

public interface AccountingEventPublisher {

    void publishCustomerInvoicePosted(CustomerInvoicePostedEvent event);
}
