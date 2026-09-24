package com.bradox.delin.purchase.service.domain.ports.output.messaging;

import com.bradox.delin.purchase.service.domain.event.VendorBillPostedEvent;
import com.bradox.delin.purchase.service.domain.event.VendorPaymentRegisteredEvent;

public interface PurchaseEventPublisher {

    void publishVendorBillPosted(VendorBillPostedEvent event);

    void publishVendorPaymentRegistered(VendorPaymentRegisteredEvent event);
}
