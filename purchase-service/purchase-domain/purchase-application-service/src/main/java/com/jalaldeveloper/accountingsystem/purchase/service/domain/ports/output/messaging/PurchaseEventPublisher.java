package com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.messaging;

import com.jalaldeveloper.accountingsystem.purchase.service.domain.event.VendorBillPostedEvent;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.event.VendorPaymentRegisteredEvent;

public interface PurchaseEventPublisher {

    void publishVendorBillPosted(VendorBillPostedEvent event);

    void publishVendorPaymentRegistered(VendorPaymentRegisteredEvent event);
}
