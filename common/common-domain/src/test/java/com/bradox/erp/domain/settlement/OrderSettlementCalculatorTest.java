package com.bradox.erp.domain.settlement;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSettlementCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);
    private static final BigDecimal ORDER_TOTAL = bd("8500000");
    private static final BigDecimal BILL = bd("8500000");
    private static final BigDecimal AFTER_RETURN = bd("7650000");
    private static final BigDecimal CREDIT = bd("850000");

    @Test
    void cancelled_returnsZeros() {
        OrderSettlement s = OrderSettlementCalculator.compute(
                true, ORDER_TOTAL, List.of(invoice(BILL, BILL, null)), TODAY);
        assertThat(s.status()).isEqualTo(SettlementStatus.CANCELLED);
        assertInvariant(s);
        assertThat(s.total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(s.paidNet()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void nothingBilled_usesOrderTotal_statusNew() {
        OrderSettlement s = OrderSettlementCalculator.compute(
                false, ORDER_TOTAL, List.of(), TODAY);
        assertThat(s.status()).isEqualTo(SettlementStatus.NEW);
        assertThat(s.total()).isEqualByComparingTo(ORDER_TOTAL);
        assertThat(s.billed()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(s.paidNet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(s.balance()).isEqualByComparingTo(ORDER_TOTAL);
        assertInvariant(s);
    }

    @Test
    void billPayFull_thenPartialReturn_creditNote_andRefund_isPaid() {
        // Bill 8.5M paid in full, then CN 850k refunded → billed 7.65M, paidNet 7.65M
        List<SettlementDoc> docs = List.of(
                invoice(BILL, BILL, TODAY.plusDays(30)),
                creditNote(CREDIT, CREDIT));
        OrderSettlement s = OrderSettlementCalculator.compute(false, ORDER_TOTAL, docs, TODAY);
        assertThat(s.billed()).isEqualByComparingTo(AFTER_RETURN);
        assertThat(s.paidNet()).isEqualByComparingTo(AFTER_RETURN);
        assertThat(s.total()).isEqualByComparingTo(AFTER_RETURN);
        assertThat(s.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(s.status()).isEqualTo(SettlementStatus.PAID);
        assertInvariant(s);
    }

    @Test
    void billPayFull_thenPartialReturn_creditNote_withoutRefund_isToRefund() {
        List<SettlementDoc> docs = List.of(
                invoice(BILL, BILL, TODAY.plusDays(30)),
                creditNote(CREDIT, BigDecimal.ZERO));
        OrderSettlement s = OrderSettlementCalculator.compute(false, ORDER_TOTAL, docs, TODAY);
        assertThat(s.billed()).isEqualByComparingTo(AFTER_RETURN);
        assertThat(s.paidNet()).isEqualByComparingTo(BILL);
        assertThat(s.balance()).isEqualByComparingTo(bd("-850000"));
        assertThat(s.status()).isEqualTo(SettlementStatus.TO_REFUND);
        assertInvariant(s);
    }

    @Test
    void partialBill_partialPay_isPartialPaid() {
        List<SettlementDoc> docs = List.of(
                invoice(bd("5000000"), bd("2000000"), TODAY.plusDays(10)),
                invoice(bd("2650000"), BigDecimal.ZERO, TODAY.plusDays(20)));
        OrderSettlement s = OrderSettlementCalculator.compute(false, ORDER_TOTAL, docs, TODAY);
        assertThat(s.billed()).isEqualByComparingTo(AFTER_RETURN);
        assertThat(s.paidNet()).isEqualByComparingTo(bd("2000000"));
        assertThat(s.balance()).isEqualByComparingTo(bd("5650000"));
        assertThat(s.status()).isEqualTo(SettlementStatus.PARTIAL_PAID);
        assertInvariant(s);
    }

    @Test
    void unpaidOverdue_isUnpaid() {
        List<SettlementDoc> docs = List.of(
                invoice(BILL, BigDecimal.ZERO, TODAY.minusDays(1)));
        OrderSettlement s = OrderSettlementCalculator.compute(false, ORDER_TOTAL, docs, TODAY);
        assertThat(s.status()).isEqualTo(SettlementStatus.UNPAID);
        assertThat(s.balance()).isEqualByComparingTo(BILL);
        assertInvariant(s);
    }

    @Test
    void unpaidNotYetDue_isNew() {
        List<SettlementDoc> docs = List.of(
                invoice(BILL, BigDecimal.ZERO, TODAY.plusDays(5)));
        OrderSettlement s = OrderSettlementCalculator.compute(false, ORDER_TOTAL, docs, TODAY);
        assertThat(s.status()).isEqualTo(SettlementStatus.NEW);
        assertInvariant(s);
    }

    @Test
    void paymentReversedAfterCreditNote_showsToRefund() {
        // Pay 8.5M, reverse payment (settled=0), CN 850k posted unpaid → billed 7.65M, paid 0, balance 7.65M NEW
        // Alternate: pay 8.5M still on books, CN unpaid → TO_REFUND (already covered).
        // Reversal after CN+refund: invoice settled 0, CN settled 0 → balance = billed
        List<SettlementDoc> docs = List.of(
                invoice(BILL, BigDecimal.ZERO, TODAY.plusDays(30)),
                creditNote(CREDIT, BigDecimal.ZERO));
        OrderSettlement s = OrderSettlementCalculator.compute(false, ORDER_TOTAL, docs, TODAY);
        assertThat(s.billed()).isEqualByComparingTo(AFTER_RETURN);
        assertThat(s.paidNet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(s.balance()).isEqualByComparingTo(AFTER_RETURN);
        assertThat(s.status()).isEqualTo(SettlementStatus.NEW);
        assertInvariant(s);
    }

    @Test
    void fullCreditNoteAfterFullPayment_refunded_isPaid() {
        List<SettlementDoc> docs = List.of(
                invoice(BILL, BILL, null),
                creditNote(BILL, BILL));
        OrderSettlement s = OrderSettlementCalculator.compute(false, ORDER_TOTAL, docs, TODAY);
        assertThat(s.billed()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(s.paidNet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(s.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(s.status()).isEqualTo(SettlementStatus.PAID);
        assertInvariant(s);
    }

    private static void assertInvariant(OrderSettlement s) {
        if (s.billed().signum() == 0
                && s.paidNet().signum() == 0
                && (s.status() == SettlementStatus.NEW || s.status() == SettlementStatus.UNPAID)) {
            // Nothing billed yet: balance (amount due) equals the display total (order total).
            assertThat(s.balance()).isEqualByComparingTo(s.total());
            return;
        }
        assertThat(s.balance()).isEqualByComparingTo(s.billed().subtract(s.paidNet()));
    }

    private static SettlementDoc invoice(BigDecimal total, BigDecimal settled, LocalDate due) {
        return new SettlementDoc(SettlementDoc.Kind.INVOICE, total, settled, due);
    }

    private static SettlementDoc creditNote(BigDecimal total, BigDecimal settled) {
        return new SettlementDoc(SettlementDoc.Kind.CREDIT_NOTE, total, settled, null);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
