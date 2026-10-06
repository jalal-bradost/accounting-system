package com.bradox.erp.sales.service.domain.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Guided sales return: quantities per order line (in the order line's unit), returned and
 * validated in one step across however many deliveries they came from.
 */
public class ReturnGoodsCommand {

    private List<Line> lines = new ArrayList<>();
    /** Refund (true, default): order reduced and invoiced part credited. Replace (false): re-delivered. */
    private boolean refund = true;
    /** Damaged goods are scrapped right away instead of going back into sellable stock. */
    private boolean damaged;
    private String reason;

    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
    public boolean isRefund() { return refund; }
    public void setRefund(boolean refund) { this.refund = refund; }
    public boolean isDamaged() { return damaged; }
    public void setDamaged(boolean damaged) { this.damaged = damaged; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static class Line {
        private UUID salesOrderLineId;
        private BigDecimal qty;

        public UUID getSalesOrderLineId() { return salesOrderLineId; }
        public void setSalesOrderLineId(UUID salesOrderLineId) { this.salesOrderLineId = salesOrderLineId; }
        public BigDecimal getQty() { return qty; }
        public void setQty(BigDecimal qty) { this.qty = qty; }
    }
}
