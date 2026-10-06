package com.bradox.erp.assistant.tools.support;

import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceLineResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceLineTaxResponse;
import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState;
import com.bradox.erp.domain.valueobject.DiscountMath;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class InvoiceToolSupport {

    private InvoiceToolSupport() {}

    public static boolean isPostedInvoice(CustomerInvoiceResponse inv) {
        return inv != null
                && inv.getState() == CustomerInvoiceState.POSTED
                && inv.getMoveType() == CustomerInvoiceMoveType.INVOICE;
    }

    /** Approximate document total in invoice currency (non-gift lines + tax − order discount). */
    public static BigDecimal documentTotal(CustomerInvoiceResponse inv) {
        if (inv == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (CustomerInvoiceLineResponse line : inv.getLines()) {
            if (line == null || line.isGift()) {
                continue;
            }
            BigDecimal lineNet = DiscountMath.lineNet(
                    line.getQty(), line.getUnitPrice(), line.getDiscountType(), line.getDiscountValue());
            total = total.add(lineNet.setScale(4, RoundingMode.HALF_UP));
            if (line.getTaxSnapshots() != null) {
                for (CustomerInvoiceLineTaxResponse tax : line.getTaxSnapshots()) {
                    if (tax != null && tax.getTaxAmount() != null) {
                        total = total.add(tax.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
                    }
                }
            }
        }
        BigDecimal orderDisc = inv.getOrderDiscountAmount() != null
                ? inv.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        return total.subtract(orderDisc).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }
}
