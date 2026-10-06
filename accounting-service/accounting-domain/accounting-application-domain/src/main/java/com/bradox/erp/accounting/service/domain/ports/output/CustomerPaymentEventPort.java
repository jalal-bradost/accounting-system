package com.bradox.erp.accounting.service.domain.ports.output;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Notifies other modules when a customer payment is reversed or replaced, so records that mirror it
 * (such as a field salesperson's cash collected) stay equal to the payment.
 */
public interface CustomerPaymentEventPort {

    /** The payment was reversed; whatever was recorded for it must be undone. */
    void onPaymentReversed(UUID companyId, UUID paymentId);

    /** The reversed payment was replaced by a corrected one for {@code newAmount}. */
    void onPaymentCorrected(UUID companyId, UUID oldPaymentId, UUID newPaymentId, BigDecimal newAmount);
}
