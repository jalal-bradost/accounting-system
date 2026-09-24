package com.bradox.erp.purchase.service.domain.ports.output.messaging;

import com.bradox.erp.purchase.service.domain.event.VendorBillPostedEvent;
import com.bradox.erp.purchase.service.domain.event.VendorPaymentRegisteredEvent;

public interface PurchaseEventPublisher {

    void publishVendorBillPosted(VendorBillPostedEvent event);

    void publishVendorPaymentRegistered(VendorPaymentRegisteredEvent event);
}
