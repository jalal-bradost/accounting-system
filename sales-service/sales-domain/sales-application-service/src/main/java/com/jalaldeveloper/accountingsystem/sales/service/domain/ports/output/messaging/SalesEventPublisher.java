package com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.messaging;

import com.jalaldeveloper.accountingsystem.sales.service.domain.event.SalesOrderConfirmedEvent;

public interface SalesEventPublisher {

    void publishSalesOrderConfirmed(SalesOrderConfirmedEvent event);
}
