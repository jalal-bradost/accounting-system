package com.bradox.erp.domain.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A posted bill/invoice or credit note contributing to order settlement.
 * {@code settled} is payments on invoices / refunds on credit notes.
 */
public record SettlementDoc(Kind kind, BigDecimal total, BigDecimal settled, LocalDate dueDate) {

    public enum Kind {
        INVOICE,
        CREDIT_NOTE
    }

    public SettlementDoc {
        Objects.requireNonNull(kind, "kind");
        total = total != null ? total : BigDecimal.ZERO;
        settled = settled != null ? settled : BigDecimal.ZERO;
    }
}
