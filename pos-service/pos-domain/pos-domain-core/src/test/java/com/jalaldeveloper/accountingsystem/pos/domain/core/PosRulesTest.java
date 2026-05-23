package com.jalaldeveloper.accountingsystem.pos.domain.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PosRulesTest {
    @Test
    void ensureCanFinalizeRejectsUnderpaidOrders() {
        assertThatThrownBy(() -> PosRules.ensureCanFinalize(
                PosOrderState.PAID,
                new BigDecimal("25.00"),
                new BigDecimal("24.99")))
                .isInstanceOf(PosDomainException.class)
                .hasMessageContaining("payments do not cover");
    }

    @Test
    void ensureCanFinalizeAcceptsPaidOrders() {
        assertThatCode(() -> PosRules.ensureCanFinalize(
                PosOrderState.PAID,
                new BigDecimal("25.00"),
                new BigDecimal("25.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureOrderDraftRejectsFinalizedOrders() {
        assertThatThrownBy(() -> PosRules.ensureOrderDraft(PosOrderState.FINALIZED))
                .isInstanceOf(PosDomainException.class)
                .hasMessageContaining("draft");
    }
}
