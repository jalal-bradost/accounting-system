package com.bradox.erp.purchase.service.domain.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * What a guided correction did (or, for a preview, would do): the order afterwards and every bill
 * and credit note it posted.
 */
public class PurchaseCorrectionResult {

    private boolean preview;
    private PurchaseOrderResponse order;
    private List<VendorBillResponse> documents = new ArrayList<>();

    public boolean isPreview() { return preview; }
    public void setPreview(boolean preview) { this.preview = preview; }
    public PurchaseOrderResponse getOrder() { return order; }
    public void setOrder(PurchaseOrderResponse order) { this.order = order; }
    public List<VendorBillResponse> getDocuments() { return documents; }
    public void setDocuments(List<VendorBillResponse> documents) { this.documents = documents; }
}
