package com.jalaldeveloper.accountingsystem.purchase.service.domain;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.PurchaseDomainException;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.PurchaseOrderRules;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.TaxAmountType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PurchaseTaxEngine {

    private PurchaseTaxEngine() {}

    public record TaxSplit(BigDecimal net, BigDecimal taxTotal, Map<UUID, BigDecimal> taxAmountById) {}

    public static TaxSplit computeLineTaxes(BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent,
                                             List<FiscalTaxSnapshot> taxesInOrder) {
        BigDecimal net = PurchaseOrderRules.lineNet(qty, unitPrice, discountPercent)
                .setScale(4, RoundingMode.HALF_UP);
        if (taxesInOrder == null || taxesInOrder.isEmpty()) {
            return new TaxSplit(net, BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), Map.of());
        }
        Map<UUID, FiscalTaxSnapshot> byId = taxesInOrder.stream()
                .collect(Collectors.toMap(FiscalTaxSnapshot::id, Function.identity(), (a, b) -> a));
        BigDecimal taxSum = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        Map<UUID, BigDecimal> perTax = new LinkedHashMap<>();
        BigDecimal runningNet = net;
        for (FiscalTaxSnapshot t : taxesInOrder) {
            FiscalTaxSnapshot tax = byId.get(t.id());
            if (tax == null) {
                throw new PurchaseDomainException("Unknown tax id on line: " + t.id());
            }
            if (tax.amountType() != TaxAmountType.PERCENT) {
                throw new PurchaseDomainException("Unsupported tax amount type: " + tax.amountType());
            }
            if (tax.priceInclude()) {
                throw new PurchaseDomainException("Tax-inclusive pricing is not implemented yet");
            }
            BigDecimal rate = tax.amount().divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
            BigDecimal amt = runningNet.multiply(rate).setScale(4, RoundingMode.HALF_UP);
            perTax.put(tax.id(), amt);
            taxSum = taxSum.add(amt);
        }
        return new TaxSplit(net, taxSum.setScale(4, RoundingMode.HALF_UP), perTax);
    }

    public static BigDecimal convertAtRate(BigDecimal documentAmount, BigDecimal rateToCompany) {
        BigDecimal r = rateToCompany != null && rateToCompany.signum() > 0 ? rateToCompany : BigDecimal.ONE;
        return documentAmount.multiply(r).setScale(4, RoundingMode.HALF_UP);
    }
}
