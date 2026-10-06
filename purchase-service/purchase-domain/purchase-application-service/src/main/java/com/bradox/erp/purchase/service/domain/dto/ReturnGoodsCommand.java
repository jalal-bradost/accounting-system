package com.bradox.erp.purchase.service.domain.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Guided return to the vendor: quantities per order line (in the order line's unit), returned and
 * validated in one step across however many receipts they came from.
 */
public class ReturnGoodsCommand {

    private List<Line> lines = new ArrayList<>();
    /** Refund (true, default): order reduced and the billed part credited. Replace (false): vendor re-delivers. */
    private boolean refund = true;
    private String reason;

    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
    public boolean isRefund() { return refund; }
    public void setRefund(boolean refund) { this.refund = refund; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static class Line {
        private UUID purchaseOrderLineId;
        private BigDecimal qty;

        public UUID getPurchaseOrderLineId() { return purchaseOrderLineId; }
        public void setPurchaseOrderLineId(UUID purchaseOrderLineId) { this.purchaseOrderLineId = purchaseOrderLineId; }
        public BigDecimal getQty() { return qty; }
        public void setQty(BigDecimal qty) { this.qty = qty; }
    }
}
