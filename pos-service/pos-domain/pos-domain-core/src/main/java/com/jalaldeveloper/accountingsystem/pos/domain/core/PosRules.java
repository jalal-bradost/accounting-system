package com.jalaldeveloper.accountingsystem.pos.domain.core;

import java.math.BigDecimal;

public final class PosRules {
    private PosRules() {}

    public static void ensureSessionOpen(PosSessionState state) {
        if (state != PosSessionState.OPEN) {
            throw new PosDomainException("error.pos.sessionNotOpen", null, "POS session is not open");
        }
    }

    public static void ensureOrderDraft(PosOrderState state) {
        if (state != PosOrderState.DRAFT) {
            throw new PosDomainException("error.pos.onlyDraftOrdersCanChange", null, "Only draft POS orders can be changed");
        }
    }

    public static void ensureCanFinalize(PosOrderState state, BigDecimal total, BigDecimal paid) {
        if (state == PosOrderState.FINALIZED) {
            throw new PosDomainException("error.pos.orderAlreadyFinalized", null, "POS order is already finalized");
        }
        if (state == PosOrderState.CANCELLED) {
            throw new PosDomainException("error.pos.cancelledOrderCannotFinalize", null, "Cancelled POS orders cannot be finalized");
        }
        if (total == null || total.signum() <= 0) {
            throw new PosDomainException("error.pos.orderTotalMustBePositive", null, "POS order total must be greater than zero");
        }
        if (paid == null || paid.compareTo(total) < 0) {
            throw new PosDomainException("error.pos.paymentsDoNotCoverTotal", null, "POS payments do not cover the order total");
        }
    }
}
