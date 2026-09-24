package com.bradox.delin.sales.service.domain.ports.output.messaging;

import com.bradox.delin.sales.service.domain.event.SalesOrderConfirmedEvent;

public interface SalesEventPublisher {

    void publishSalesOrderConfirmed(SalesOrderConfirmedEvent event);
}
