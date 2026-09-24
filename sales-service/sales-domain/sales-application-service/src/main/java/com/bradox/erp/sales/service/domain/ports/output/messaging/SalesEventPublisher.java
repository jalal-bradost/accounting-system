package com.bradox.erp.sales.service.domain.ports.output.messaging;

import com.bradox.erp.sales.service.domain.event.SalesOrderConfirmedEvent;

public interface SalesEventPublisher {

    void publishSalesOrderConfirmed(SalesOrderConfirmedEvent event);
}
