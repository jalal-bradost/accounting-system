package com.bradox.erp.sales.service.domain.dto;

import com.bradox.erp.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * What a guided correction did (or, for a preview, would do): the order afterwards and every
 * invoice and credit note it posted.
 */
public class SalesCorrectionResult {

    private boolean preview;
    private SalesOrderResponse order;
    private List<CustomerInvoiceResponse> documents = new ArrayList<>();

    public boolean isPreview() { return preview; }
    public void setPreview(boolean preview) { this.preview = preview; }
    public SalesOrderResponse getOrder() { return order; }
    public void setOrder(SalesOrderResponse order) { this.order = order; }
    public List<CustomerInvoiceResponse> getDocuments() { return documents; }
    public void setDocuments(List<CustomerInvoiceResponse> documents) { this.documents = documents; }
}
