package com.bradox.erp.purchase.service.domain;

import com.bradox.erp.domain.valueobject.DiscountMath;
import com.bradox.erp.purchase.domain.core.PurchaseOrderRules;
import com.bradox.erp.purchase.domain.core.entity.VendorBill;
import com.bradox.erp.purchase.domain.core.entity.VendorBillLine;
import com.bradox.erp.purchase.domain.core.entity.VendorBillLineTax;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Document-currency totals of vendor bills / credit notes. */
public final class VendorBillMath {

    private VendorBillMath() {}

    /** Amount due on the document (lines net + taxes − order discount), never negative. */
    public static BigDecimal total(VendorBill bill) {
        BigDecimal total = BigDecimal.ZERO;
        for (VendorBillLine line : bill.getLines()) {
            total = total.add(lineNet(line).setScale(4, RoundingMode.HALF_UP));
            for (VendorBillLineTax ts : line.getTaxSnapshots()) {
                total = total.add(ts.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
            }
        }
        BigDecimal orderDisc = bill.getOrderDiscountAmount() != null
                ? bill.getOrderDiscountAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        return total.subtract(orderDisc).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal lineGross(VendorBillLine line) {
        if (line.getQty() == null || line.getUnitPrice() == null) {
            return BigDecimal.ZERO;
        }
        return line.getQty().multiply(line.getUnitPrice()).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal lineDiscount(VendorBillLine line) {
        return DiscountMath.discountAmount(lineGross(line), line.getDiscountType(), line.getDiscountValue())
                .setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal lineNet(VendorBillLine line) {
        return PurchaseOrderRules
                .lineNet(line.getQty(), line.getUnitPrice(), line.getDiscountType(), line.getDiscountValue())
                .setScale(4, RoundingMode.HALF_UP);
    }
}
