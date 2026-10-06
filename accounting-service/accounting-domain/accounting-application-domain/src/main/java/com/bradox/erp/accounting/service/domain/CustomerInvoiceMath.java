package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.domain.core.entity.CustomerInvoice;
import com.bradox.erp.domain.core.entity.CustomerInvoiceLine;
import com.bradox.erp.domain.core.entity.CustomerInvoiceLineTax;
import com.bradox.erp.domain.valueobject.DiscountMath;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Document-currency totals of customer invoices / credit notes. */
public final class CustomerInvoiceMath {

    private CustomerInvoiceMath() {}

    /** Amount due on the document (lines net + taxes − order discount), never negative. */
    public static BigDecimal total(CustomerInvoice inv) {
        BigDecimal total = BigDecimal.ZERO;
        for (CustomerInvoiceLine line : inv.getLines()) {
            if (line.isGift()) {
                continue;
            }
            total = total.add(lineNet(line).setScale(4, RoundingMode.HALF_UP));
            for (CustomerInvoiceLineTax ts : line.getTaxSnapshots()) {
                total = total.add(ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
            }
        }
        BigDecimal orderDisc = inv.getOrderDiscountAmount() != null
                ? inv.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        return total.subtract(orderDisc).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal lineGross(CustomerInvoiceLine line) {
        if (line.getQty() == null || line.getUnitPrice() == null) {
            return BigDecimal.ZERO;
        }
        return line.getQty().multiply(line.getUnitPrice()).max(BigDecimal.ZERO);
    }

    public static BigDecimal lineDiscount(CustomerInvoiceLine line) {
        if (line.isGift()) {
            return BigDecimal.ZERO;
        }
        return DiscountMath.discountAmount(lineGross(line), line.getDiscountType(), line.getDiscountValue());
    }

    public static BigDecimal lineNet(CustomerInvoiceLine line) {
        if (line.isGift()) {
            return BigDecimal.ZERO;
        }
        return DiscountMath.lineNet(line.getQty(), line.getUnitPrice(), line.getDiscountType(), line.getDiscountValue());
    }
}
