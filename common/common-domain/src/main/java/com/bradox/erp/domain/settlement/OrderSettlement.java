package com.bradox.erp.domain.settlement;

import java.math.BigDecimal;

/**
 * Computed settlement figures for a sales or purchase order.
 *
 * <ul>
 *   <li>{@code total} — net billed once anything is billed; otherwise the order total</li>
 *   <li>{@code billed} — sum(invoices) − sum(credit notes)</li>
 *   <li>{@code paidNet} — sum(payments) − sum(refunds)</li>
 *   <li>{@code balance} — billed − paidNet (may be negative when a refund is owed)</li>
 * </ul>
 */
public record OrderSettlement(
        BigDecimal total,
        BigDecimal billed,
        BigDecimal paidNet,
        BigDecimal balance,
        SettlementStatus status
) {
}
