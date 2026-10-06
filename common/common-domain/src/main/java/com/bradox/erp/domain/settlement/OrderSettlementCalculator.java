package com.bradox.erp.domain.settlement;

import com.bradox.erp.domain.valueobject.MonetaryScale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Pure calculator for order payment settlement from posted accounting documents.
 * Shared by purchase and sales so totals stay consistent through returns and refunds.
 */
public final class OrderSettlementCalculator {

    private static final BigDecimal EPS = new BigDecimal("0.005");

    private OrderSettlementCalculator() {}

    /**
     * @param cancelled  whether the order is cancelled
     * @param orderTotal the order's own amountTotal (used when nothing is billed yet)
     * @param docs       posted invoices and credit notes with their settled amounts
     * @param today      reference date for overdue detection (typically LocalDate.now())
     */
    public static OrderSettlement compute(
            boolean cancelled,
            BigDecimal orderTotal,
            List<SettlementDoc> docs,
            LocalDate today) {
        if (cancelled) {
            return zero(SettlementStatus.CANCELLED);
        }

        BigDecimal order = orderTotal != null ? orderTotal : BigDecimal.ZERO;
        List<SettlementDoc> safeDocs = docs != null ? docs : List.of();

        if (safeDocs.isEmpty()) {
            SettlementStatus status = SettlementStatus.NEW;
            return new OrderSettlement(
                    scale(order),
                    scale(BigDecimal.ZERO),
                    scale(BigDecimal.ZERO),
                    scale(order),
                    status);
        }

        BigDecimal invoiceTotal = BigDecimal.ZERO;
        BigDecimal creditNoteTotal = BigDecimal.ZERO;
        BigDecimal payments = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        LocalDate earliestDue = null;

        for (SettlementDoc doc : safeDocs) {
            if (doc.kind() == SettlementDoc.Kind.INVOICE) {
                invoiceTotal = invoiceTotal.add(doc.total());
                payments = payments.add(doc.settled());
                if (doc.dueDate() != null && (earliestDue == null || doc.dueDate().isBefore(earliestDue))) {
                    earliestDue = doc.dueDate();
                }
            } else {
                creditNoteTotal = creditNoteTotal.add(doc.total());
                refunds = refunds.add(doc.settled());
            }
        }

        BigDecimal billed = scale(invoiceTotal.subtract(creditNoteTotal));
        BigDecimal paidNet = scale(payments.subtract(refunds));
        BigDecimal balance = scale(billed.subtract(paidNet));
        // Display total: net billed once documents exist
        BigDecimal total = billed;

        SettlementStatus status = resolveStatus(paidNet, balance, earliestDue, today);

        return new OrderSettlement(total, billed, paidNet, balance, status);
    }

    private static SettlementStatus resolveStatus(
            BigDecimal paidNet,
            BigDecimal balance,
            LocalDate earliestDue,
            LocalDate today) {
        if (balance.compareTo(EPS.negate()) < 0) {
            return SettlementStatus.TO_REFUND;
        }
        if (balance.abs().compareTo(EPS) <= 0) {
            // Settled (or near-zero). If nothing was ever paid, treat as NEW/UNPAID only when balance is the full amount —
            // but here balance ≈ 0 means PAID (including zero-total edge cases after full credit).
            return SettlementStatus.PAID;
        }
        // balance > EPS: still owed
        if (paidNet.compareTo(EPS) <= 0) {
            boolean overdue = earliestDue != null
                    && today != null
                    && earliestDue.isBefore(today);
            return overdue ? SettlementStatus.UNPAID : SettlementStatus.NEW;
        }
        return SettlementStatus.PARTIAL_PAID;
    }

    private static OrderSettlement zero(SettlementStatus status) {
        BigDecimal z = scale(BigDecimal.ZERO);
        return new OrderSettlement(z, z, z, z, status);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(MonetaryScale.SCALE, RoundingMode.HALF_UP);
    }
}
