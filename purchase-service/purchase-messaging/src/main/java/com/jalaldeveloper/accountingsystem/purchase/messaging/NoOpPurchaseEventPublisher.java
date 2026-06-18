package com.jalaldeveloper.accountingsystem.purchase.messaging;

import com.jalaldeveloper.accountingsystem.purchase.service.domain.event.VendorBillPostedEvent;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.event.VendorPaymentRegisteredEvent;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.messaging.PurchaseEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPurchaseEventPublisher implements PurchaseEventPublisher {

    @Override
    public void publishVendorBillPosted(VendorBillPostedEvent event) {
        // Messaging disabled — purchase events are not published.
    }

    @Override
    public void publishVendorPaymentRegistered(VendorPaymentRegisteredEvent event) {
        // Messaging disabled — purchase events are not published.
    }
}
