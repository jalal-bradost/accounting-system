package com.bradox.erp;

import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end purchase flow: PO save (draft), confirm, receive, create & post bill,
 * then register payment with AP reconciliation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PurchaseApiIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void rfqConfirm_receive_bill_pay_happyPath() throws Exception {
        UUID apAccountId = accountIdByCode("430004");
        UUID vatAccountId = accountIdByCode("430013");
        UUID bankJournalId = journalIdByType("BANK");

        UUID vendorId = createVendor(apAccountId);

        UUID taxId = createFiscalTax(vatAccountId);

        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("PUR-" + UUID.randomUUID().toString().substring(0, 8),
                "Purchase Test Item", categoryId, uomId, "5.00", "12.00");

        String poBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line1\",\"uomId\":\""
                + uomId + "\",\"qtyOrdered\":3,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[\"" + taxId + "\"]}]}";
        MvcResult poResult = mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode po = json.readTree(poResult.getResponse().getContentAsString());
        UUID poId = UUID.fromString(po.get("id").asText());
        assertThat(po.get("state").asText()).isEqualTo("DRAFT");
        po = confirmPurchaseOrder(poId);
        assertThat(po.get("state").asText()).isEqualTo("CONFIRMED");
        assertThat(po.get("lines").get(0).get("qtyReceived").decimalValue())
                .isEqualByComparingTo(new BigDecimal("3"));
        assertThat(po.get("canCreateVendorBill").asBoolean()).isFalse();

        MvcResult billsListRes = mockMvc.perform(get("/api/v1/purchase/vendor-bills")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode billsArr = json.readTree(billsListRes.getResponse().getContentAsString());
        UUID billId = null;
        for (JsonNode b : billsArr) {
            if (poId.toString().equals(b.get("purchaseOrderId").asText())
                    && "POSTED".equals(b.get("state").asText())) {
                billId = UUID.fromString(b.get("id").asText());
                break;
            }
        }
        assertThat(billId).isNotNull();

        MvcResult getBillRes = mockMvc.perform(get("/api/v1/purchase/vendor-bills/" + billId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode billDetail = json.readTree(getBillRes.getResponse().getContentAsString());
        assertThat(billDetail.get("lines").isArray()).isTrue();
        assertThat(billDetail.get("lines").size()).isPositive();
        UUID journalEntryId = UUID.fromString(billDetail.get("journalEntryId").asText());

        MvcResult jeResult = mockMvc.perform(get("/api/v1/journal-entries/" + journalEntryId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode je = json.readTree(jeResult.getResponse().getContentAsString());
        BigDecimal apCredit = BigDecimal.ZERO;
        for (JsonNode it : je.get("items")) {
            if (it.get("accountId").asText().equals(apAccountId.toString())) {
                apCredit = apCredit.add(it.get("credit").decimalValue());
            }
        }
        assertThat(apCredit).isPositive();

        String payBody = "{\"vendorBillId\":\"" + billId + "\",\"bankJournalId\":\"" + bankJournalId
                + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":" + apCredit.toPlainString()
                + ",\"currencyCode\":\"USD\",\"reference\":\"TEST-PAY\"}";
        MvcResult payRes = mockMvc.perform(post("/api/v1/purchase/vendor-payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pay = json.readTree(payRes.getResponse().getContentAsString());
        assertThat(pay.get("reconciliationId").asText()).isNotBlank();

        MvcResult paysListRes = mockMvc.perform(get("/api/v1/purchase/vendor-payments")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode paysArr = json.readTree(paysListRes.getResponse().getContentAsString());
        assertThat(paysArr.isArray()).isTrue();
        boolean payInList = false;
        for (JsonNode p : paysArr) {
            if (billId.toString().equals(p.get("vendorBillId").asText())) {
                payInList = true;
                break;
            }
        }
        assertThat(payInList).isTrue();
    }

    @Test
    void duplicate_draft_vendor_bill_from_same_po_is_rejected() throws Exception {
        UUID apAccountId = accountIdByCode("430004");
        UUID vatAccountId = accountIdByCode("430013");
        UUID vendorId = createVendor(apAccountId);
        UUID taxId = createFiscalTax(vatAccountId);
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("PUR-DUP-" + UUID.randomUUID().toString().substring(0, 8),
                "Duplicate bill test", categoryId, uomId, "5.00", "12.00");

        String poBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line1\",\"uomId\":\""
                + uomId + "\",\"qtyOrdered\":2,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[\"" + taxId + "\"]}]}";
        JsonNode po = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID poId = UUID.fromString(po.get("id").asText());
        assertThat(po.get("state").asText()).isEqualTo("DRAFT");
        po = confirmPurchaseOrder(poId);
        assertThat(po.get("state").asText()).isEqualTo("CONFIRMED");
        assertThat(po.get("canCreateVendorBill").asBoolean()).isFalse();

        String billBody = "{\"purchaseOrderId\":\"" + poId + "\",\"billDate\":\"2026-05-04\"}";
        mockMvc.perform(post("/api/v1/purchase/vendor-bills/from-po")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billBody))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void vendor_payment_cannot_exceed_outstanding_balance() throws Exception {
        UUID apAccountId = accountIdByCode("430004");
        UUID vatAccountId = accountIdByCode("430013");
        UUID bankJournalId = journalIdByType("BANK");
        UUID vendorId = createVendor(apAccountId);
        UUID taxId = createFiscalTax(vatAccountId);
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("PUR-PAY-" + UUID.randomUUID().toString().substring(0, 8),
                "Overpay test", categoryId, uomId, "5.00", "12.00");

        String poBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line1\",\"uomId\":\""
                + uomId + "\",\"qtyOrdered\":1,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[\"" + taxId + "\"]}]}";
        JsonNode po = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID poId = UUID.fromString(po.get("id").asText());
        confirmPurchaseOrder(poId);

        JsonNode billsArr = json.readTree(mockMvc.perform(get("/api/v1/purchase/vendor-bills")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID billId = null;
        for (JsonNode b : billsArr) {
            if (poId.toString().equals(b.get("purchaseOrderId").asText())
                    && "POSTED".equals(b.get("state").asText())) {
                billId = UUID.fromString(b.get("id").asText());
                break;
            }
        }
        assertThat(billId).isNotNull();

        JsonNode billDetail = json.readTree(mockMvc.perform(get("/api/v1/purchase/vendor-bills/" + billId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID journalEntryId = UUID.fromString(billDetail.get("journalEntryId").asText());

        JsonNode je = json.readTree(mockMvc.perform(get("/api/v1/journal-entries/" + journalEntryId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        BigDecimal apCredit = BigDecimal.ZERO;
        for (JsonNode it : je.get("items")) {
            if (it.get("accountId").asText().equals(apAccountId.toString())) {
                apCredit = apCredit.add(it.get("credit").decimalValue());
            }
        }

        String payBody = "{\"vendorBillId\":\"" + billId + "\",\"bankJournalId\":\"" + bankJournalId
                + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":" + apCredit.toPlainString()
                + ",\"currencyCode\":\"USD\",\"reference\":\"PAY-1\"}";
        mockMvc.perform(post("/api/v1/purchase/vendor-payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/purchase/vendor-payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Confirming a PO receives into stock; qty_received is updated immediately.
     */
    @Test
    void inventory_validateIncomingReceipt_syncs_po_qty_received() throws Exception {
        UUID apAccountId = accountIdByCode("430004");
        UUID vatAccountId = accountIdByCode("430013");
        UUID vendorId = createVendor(apAccountId);
        UUID taxId = createFiscalTax(vatAccountId);
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("PUR-INV-" + UUID.randomUUID().toString().substring(0, 8),
                "Inventory validate sync item", categoryId, uomId, "5.00", "12.00");

        String poBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line1\",\"uomId\":\""
                + uomId + "\",\"qtyOrdered\":4,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[\"" + taxId + "\"]}]}";
        MvcResult poResult = mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode po = json.readTree(poResult.getResponse().getContentAsString());
        assertThat(po.get("state").asText()).isEqualTo("DRAFT");
        UUID poId = UUID.fromString(po.get("id").asText());
        po = confirmPurchaseOrder(poId);
        assertThat(po.get("state").asText()).isEqualTo("CONFIRMED");
        assertThat(po.get("lines").get(0).get("qtyReceived").decimalValue())
                .isEqualByComparingTo(new BigDecimal("4"));
    }

    @Test
    void vendor_return_auto_creates_draft_credit_note() throws Exception {
        UUID apAccountId = accountIdByCode("430004");
        UUID vendorId = createVendor(apAccountId);
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("PUR-RET-" + UUID.randomUUID().toString().substring(0, 8),
                "Vendor return CN", categoryId, uomId, "5.00", "12.00");

        String poBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line1\",\"uomId\":\""
                + uomId + "\",\"qtyOrdered\":2,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode po = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID poId = UUID.fromString(po.get("id").asText());
        assertThat(po.get("state").asText()).isEqualTo("DRAFT");
        po = confirmPurchaseOrder(poId);
        assertThat(po.get("state").asText()).isEqualTo("CONFIRMED");
        UUID receiptPickingId = UUID.fromString(po.get("receiptPickingIds").get(0).asText());

        MvcResult billsListRes = mockMvc.perform(get("/api/v1/purchase/vendor-bills")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        UUID billId = null;
        for (JsonNode b : json.readTree(billsListRes.getResponse().getContentAsString())) {
            if (poId.toString().equals(b.get("purchaseOrderId").asText())
                    && "POSTED".equals(b.get("state").asText())) {
                billId = UUID.fromString(b.get("id").asText());
                break;
            }
        }
        assertThat(billId).isNotNull();

        JsonNode returnPicking = json.readTree(mockMvc.perform(
                        post("/api/v1/inventory/pickings/" + receiptPickingId + "/return")
                                .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID returnPickingId = UUID.fromString(returnPicking.get("id").asText());
        UUID returnMoveId = firstMoveId(returnPickingId);
        validatePickingWithPicks(returnPickingId, returnMoveId, "1", false);

        JsonNode creditNotes = json.readTree(mockMvc.perform(
                        get("/api/v1/accounting/vendor-bills/" + billId + "/credit-notes")
                                .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(creditNotes).hasSize(1);
        assertThat(creditNotes.get(0).get("state").asText()).isEqualTo("DRAFT");
        assertThat(creditNotes.get(0).get("moveType").asText()).isEqualTo("CREDIT_NOTE");
        assertThat(creditNotes.get(0).get("reversedBillId").asText()).isEqualTo(billId.toString());
    }

    @Test
    void confirmed_po_qty_increase_creates_extra_receipt_and_bill() throws Exception {
        UUID apAccountId = accountIdByCode("430004");
        UUID vendorId = createVendor(apAccountId);
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("PUR-UP-" + UUID.randomUUID().toString().substring(0, 8),
                "Qty increase item", categoryId, uomId, "5.00", "12.00");

        String poBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line1\",\"uomId\":\""
                + uomId + "\",\"qtyOrdered\":20,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode po = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID poId = UUID.fromString(po.get("id").asText());
        po = confirmPurchaseOrder(poId);
        assertThat(po.get("state").asText()).isEqualTo("CONFIRMED");
        assertThat(po.get("receiptPickingIds")).hasSize(1);
        assertThat(po.get("canCreateReturn").asBoolean()).isTrue();
        String lineId = po.get("lines").get(0).get("id").asText();

        String amendBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"id\":\"" + lineId + "\",\"productId\":\"" + productId
                + "\",\"name\":\"Line1\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":30,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[]}]}";
        po = json.readTree(mockMvc.perform(put("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(amendBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(po.get("state").asText()).isEqualTo("CONFIRMED");
        assertThat(po.get("lines").get(0).get("qtyOrdered").decimalValue())
                .isEqualByComparingTo(new BigDecimal("30"));
        assertThat(po.get("receiptPickingIds").size()).isGreaterThanOrEqualTo(2);

        po = receiveAndBillPurchaseOrder(po);
        assertThat(po.get("lines").get(0).get("qtyReceived").decimalValue())
                .isEqualByComparingTo(new BigDecimal("30"));

        int postedBills = 0;
        for (JsonNode b : json.readTree(mockMvc.perform(get("/api/v1/purchase/vendor-bills")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())) {
            if (poId.toString().equals(b.get("purchaseOrderId").asText())
                    && "POSTED".equals(b.get("state").asText())
                    && !"CREDIT_NOTE".equals(b.path("moveType").asText())) {
                postedBills++;
            }
        }
        assertThat(postedBills).isGreaterThanOrEqualTo(2);
    }

    @Test
    void confirmed_po_qty_decrease_creates_return_and_credit_note() throws Exception {
        UUID apAccountId = accountIdByCode("430004");
        UUID vendorId = createVendor(apAccountId);
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("PUR-DN-" + UUID.randomUUID().toString().substring(0, 8),
                "Qty decrease item", categoryId, uomId, "5.00", "12.00");

        String poBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line1\",\"uomId\":\""
                + uomId + "\",\"qtyOrdered\":20,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode po = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID poId = UUID.fromString(po.get("id").asText());
        po = confirmPurchaseOrder(poId);
        String lineId = po.get("lines").get(0).get("id").asText();
        UUID billId = null;
        for (JsonNode b : json.readTree(mockMvc.perform(get("/api/v1/purchase/vendor-bills")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())) {
            if (poId.toString().equals(b.get("purchaseOrderId").asText())
                    && "POSTED".equals(b.get("state").asText())
                    && !"CREDIT_NOTE".equals(b.path("moveType").asText())) {
                billId = UUID.fromString(b.get("id").asText());
                break;
            }
        }
        assertThat(billId).isNotNull();

        String amendBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"id\":\"" + lineId + "\",\"productId\":\"" + productId
                + "\",\"name\":\"Line1\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":10,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[]}]}";
        po = json.readTree(mockMvc.perform(put("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(amendBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(po.get("state").asText()).isEqualTo("CONFIRMED");
        assertThat(po.get("lines").get(0).get("qtyOrdered").decimalValue())
                .isEqualByComparingTo(new BigDecimal("10"));
        assertThat(po.get("returnPickingIds").size()).isGreaterThanOrEqualTo(1);

        po = validateOpenReturnPickings(po);
        assertThat(po.get("lines").get(0).get("qtyReceived").decimalValue())
                .isEqualByComparingTo(new BigDecimal("10"));

        JsonNode creditNotes = json.readTree(mockMvc.perform(
                        get("/api/v1/accounting/vendor-bills/" + billId + "/credit-notes")
                                .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(creditNotes).hasSize(1);
        assertThat(creditNotes.get(0).get("state").asText()).isEqualTo("DRAFT");
        assertThat(creditNotes.get(0).get("moveType").asText()).isEqualTo("CREDIT_NOTE");
    }

    @Test
    void draft_vendor_bill_can_be_updated_and_cancelled() throws Exception {
        UUID apAccountId = accountIdByCode("430004");
        UUID vendorId = createVendor(apAccountId);
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("PUR-ED-" + UUID.randomUUID().toString().substring(0, 8),
                "Draft bill edit", categoryId, uomId, "5.00", "12.00");

        String poBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line1\",\"uomId\":\""
                + uomId + "\",\"qtyOrdered\":5,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode po = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID poId = UUID.fromString(po.get("id").asText());
        mockMvc.perform(post("/api/v1/purchase/orders/" + poId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        po = json.readTree(mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID pickingId = UUID.fromString(po.get("receiptPickingIds").get(0).asText());
        mockMvc.perform(post("/api/v1/purchase/receipts/" + pickingId + "/validate")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        JsonNode bill = json.readTree(mockMvc.perform(post("/api/v1/purchase/vendor-bills/from-po")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purchaseOrderId\":\"" + poId + "\",\"billDate\":\"2026-05-04\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID billId = UUID.fromString(bill.get("id").asText());
        assertThat(bill.get("state").asText()).isEqualTo("DRAFT");
        String lineId = bill.get("lines").get(0).get("id").asText();

        String updateBody = "{\"billDate\":\"2026-05-05\",\"dueDate\":\"2026-06-05\",\"reference\":\"VB-EDIT\","
                + "\"orderDiscountAmount\":0,\"lines\":[{\"lineId\":\"" + lineId
                + "\",\"qty\":4,\"unitPrice\":12,\"discountType\":\"PERCENT\",\"discountValue\":0}]}";
        bill = json.readTree(mockMvc.perform(put("/api/v1/purchase/vendor-bills/" + billId)
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(bill.get("reference").asText()).isEqualTo("VB-EDIT");
        assertThat(bill.get("lines").get(0).get("qty").decimalValue())
                .isEqualByComparingTo(new BigDecimal("4"));
        assertThat(bill.get("lines").get(0).get("unitPrice").decimalValue())
                .isEqualByComparingTo(new BigDecimal("12"));

        bill = json.readTree(mockMvc.perform(post("/api/v1/purchase/vendor-bills/" + billId + "/cancel")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(bill.get("state").asText()).isEqualTo("CANCELLED");
    }

    @Test
    void posted_bill_debit_note_and_po_lock() throws Exception {
        UUID apAccountId = accountIdByCode("430004");
        UUID bankJournalId = journalIdByType("BANK");
        UUID vendorId = createVendor(apAccountId);
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("PUR-LK-" + UUID.randomUUID().toString().substring(0, 8),
                "Debit lock item", categoryId, uomId, "5.00", "12.00");

        String poBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line1\",\"uomId\":\""
                + uomId + "\",\"qtyOrdered\":2,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode po = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID poId = UUID.fromString(po.get("id").asText());
        po = confirmPurchaseOrder(poId);

        UUID billId = null;
        for (JsonNode b : json.readTree(mockMvc.perform(get("/api/v1/purchase/vendor-bills")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())) {
            if (poId.toString().equals(b.get("purchaseOrderId").asText())
                    && "POSTED".equals(b.get("state").asText())
                    && "BILL".equals(b.path("moveType").asText("BILL"))) {
                billId = UUID.fromString(b.get("id").asText());
                break;
            }
        }
        assertThat(billId).isNotNull();
        JsonNode billDetail = json.readTree(mockMvc.perform(get("/api/v1/purchase/vendor-bills/" + billId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String sourceLineId = billDetail.get("lines").get(0).get("id").asText();

        JsonNode dn = json.readTree(mockMvc.perform(post("/api/v1/purchase/vendor-bills/" + billId + "/debit-note")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billDate\":\"2026-05-10\",\"lines\":[{\"billLineId\":\"" + sourceLineId
                                + "\",\"qty\":1,\"unitPrice\":3}]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(dn.get("moveType").asText()).isEqualTo("DEBIT_NOTE");
        assertThat(dn.get("state").asText()).isEqualTo("DRAFT");
        UUID dnId = UUID.fromString(dn.get("id").asText());
        mockMvc.perform(post("/api/v1/purchase/vendor-bills/" + dnId + "/post")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        po = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders/" + poId + "/lock")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(po.get("locked").asBoolean()).isTrue();

        String lineId = po.get("lines").get(0).get("id").asText();
        String amendBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"id\":\"" + lineId + "\",\"productId\":\"" + productId
                + "\",\"name\":\"Line1\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":3,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[]}]}";
        mockMvc.perform(put("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(amendBody))
                .andExpect(status().is4xxClientError());

        po = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders/" + poId + "/unlock")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(po.get("locked").asBoolean()).isFalse();
        assertThat(bankJournalId).isNotNull();
    }

    @Test
    void purchase_order_return_endpoint_creates_draft_return() throws Exception {
        UUID apAccountId = accountIdByCode("430004");
        UUID vendorId = createVendor(apAccountId);
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("PUR-OR-" + UUID.randomUUID().toString().substring(0, 8),
                "Order return button", categoryId, uomId, "5.00", "12.00");

        String poBody = "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\""
                + warehouse + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line1\",\"uomId\":\""
                + uomId + "\",\"qtyOrdered\":5,\"unitPrice\":10,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode po = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID poId = UUID.fromString(po.get("id").asText());
        confirmPurchaseOrder(poId);

        JsonNode ret = json.readTree(mockMvc.perform(post("/api/v1/purchase/orders/" + poId + "/return")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(ret.get("pickingType").asText()).isEqualTo("OUTGOING");
        assertThat(ret.get("state").asText()).isEqualTo("DRAFT");
        assertThat(ret.get("purchaseOrderId").asText()).isEqualTo(poId.toString());
    }

    private void validatePickingWithPicks(UUID id, UUID moveId, String picked, boolean createBackorder) throws Exception {
        String body = "{\"createBackorder\":" + createBackorder + ",\"picks\":[{\"moveId\":\"" + moveId
                + "\",\"pickedQuantity\":" + picked + "}]}";
        mockMvc.perform(post("/api/v1/inventory/pickings/" + id + "/validate")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private UUID firstMoveId(UUID pickingId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/inventory/pickings/" + pickingId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(json.readTree(result.getResponse().getContentAsString())
                .get("moves").get(0).get("id").asText());
    }

    private JsonNode confirmPurchaseOrder(UUID poId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/purchase/orders/" + poId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode po = json.readTree(result.getResponse().getContentAsString());
        return receiveAndBillPurchaseOrder(po);
    }

    /** Manual steps after confirm: validate open receipts, then create and post the vendor bill. */
    private JsonNode receiveAndBillPurchaseOrder(JsonNode po) throws Exception {
        UUID poId = UUID.fromString(po.get("id").asText());
        if (po.has("receiptPickingIds") && po.get("receiptPickingIds").isArray()) {
            for (JsonNode pickingIdNode : po.get("receiptPickingIds")) {
                UUID pickingId = UUID.fromString(pickingIdNode.asText());
                JsonNode picking = json.readTree(mockMvc.perform(get("/api/v1/inventory/pickings/" + pickingId)
                                .header("X-Company-Id", COMPANY_ID.toString()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
                String state = picking.get("state").asText();
                if ("DONE".equals(state) || "CANCELLED".equals(state)) {
                    continue;
                }
                mockMvc.perform(post("/api/v1/purchase/receipts/" + pickingId + "/validate")
                                .header("X-Company-Id", COMPANY_ID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());
            }
        }
        MvcResult refreshed = mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        po = json.readTree(refreshed.getResponse().getContentAsString());
        if (po.has("canCreateVendorBill") && po.get("canCreateVendorBill").asBoolean()) {
            String billBody = "{\"purchaseOrderId\":\"" + poId + "\",\"billDate\":\"2026-05-04\"}";
            MvcResult billRes = mockMvc.perform(post("/api/v1/purchase/vendor-bills/from-po")
                            .header("X-Company-Id", COMPANY_ID.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(billBody))
                    .andExpect(status().isOk())
                    .andReturn();
            UUID billId = UUID.fromString(json.readTree(billRes.getResponse().getContentAsString()).get("id").asText());
            mockMvc.perform(post("/api/v1/purchase/vendor-bills/" + billId + "/post")
                            .header("X-Company-Id", COMPANY_ID.toString()))
                    .andExpect(status().isOk());
        }
        refreshed = mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(refreshed.getResponse().getContentAsString());
    }

    private JsonNode validateOpenReturnPickings(JsonNode po) throws Exception {
        UUID poId = UUID.fromString(po.get("id").asText());
        if (po.has("returnPickingIds") && po.get("returnPickingIds").isArray()) {
            for (JsonNode pickingIdNode : po.get("returnPickingIds")) {
                UUID pickingId = UUID.fromString(pickingIdNode.asText());
                JsonNode picking = json.readTree(mockMvc.perform(get("/api/v1/inventory/pickings/" + pickingId)
                                .header("X-Company-Id", COMPANY_ID.toString()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
                String state = picking.get("state").asText();
                if ("DONE".equals(state) || "CANCELLED".equals(state)) {
                    continue;
                }
                mockMvc.perform(post("/api/v1/inventory/pickings/" + pickingId + "/validate")
                                .header("X-Company-Id", COMPANY_ID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());
            }
        }
        MvcResult refreshed = mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(refreshed.getResponse().getContentAsString());
    }

    private UUID createVendor(UUID payableAccountId) throws Exception {
        String body = "{\"kind\":\"COMPANY\",\"displayName\":\"Vendor " + UUID.randomUUID().toString().substring(0, 6)
                + "\",\"customer\":false,\"vendor\":true,\"payableAccountId\":\"" + payableAccountId
                + "\",\"currencyCode\":\"USD\"}";
        MvcResult r = mockMvc.perform(post("/api/v1/contacts/partners")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(json.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createFiscalTax(UUID vatAccountId) throws Exception {
        String body = "{\"name\":\"VAT 10%\",\"amountType\":\"PERCENT\",\"amount\":10,\"priceInclude\":false,"
                + "\"scope\":\"PURCHASE\",\"accountId\":\"" + vatAccountId + "\"}";
        MvcResult r = mockMvc.perform(post("/api/v1/purchase/fiscal-taxes")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(json.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID accountIdByCode(String code) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/accounts").param("companyId", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode n : json.readTree(r.getResponse().getContentAsString())) {
            if (code.equals(n.get("code").asText())) {
                return UUID.fromString(n.get("id").asText());
            }
        }
        throw new AssertionError("account not found: " + code);
    }

    private UUID journalIdByType(String type) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/journals").param("companyId", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode n : json.readTree(r.getResponse().getContentAsString())) {
            if (type.equalsIgnoreCase(n.get("journalType").asText())) {
                return UUID.fromString(n.get("id").asText());
            }
        }
        throw new AssertionError("journal type not found: " + type);
    }

    private UUID lookupWarehouseByCode(String code) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/inventory/warehouses")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode node : json.readTree(result.getResponse().getContentAsString())) {
            if (code.equalsIgnoreCase(node.get("code").asText())) {
                return UUID.fromString(node.get("id").asText());
            }
        }
        throw new AssertionError("warehouse not found: " + code);
    }

    private UUID lookupCategoryByName(String name) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/inventory/product-categories")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode node : json.readTree(result.getResponse().getContentAsString())) {
            if (name.equalsIgnoreCase(node.get("name").asText())) {
                return UUID.fromString(node.get("id").asText());
            }
        }
        throw new AssertionError("category not found: " + name);
    }

    private UUID lookupUomByName(String name) throws Exception {
        MvcResult catsResult = mockMvc.perform(get("/api/v1/inventory/uom-categories")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode catNode : json.readTree(catsResult.getResponse().getContentAsString())) {
            UUID catId = UUID.fromString(catNode.get("id").asText());
            MvcResult uomsResult = mockMvc.perform(get("/api/v1/inventory/uom-categories/" + catId + "/uoms")
                            .header("X-Company-Id", COMPANY_ID.toString()))
                    .andExpect(status().isOk())
                    .andReturn();
            for (JsonNode uomNode : json.readTree(uomsResult.getResponse().getContentAsString())) {
                if (name.equalsIgnoreCase(uomNode.get("name").asText())) {
                    return UUID.fromString(uomNode.get("id").asText());
                }
            }
        }
        throw new AssertionError("uom not found: " + name);
    }

    private UUID createProduct(String sku, String name, UUID categoryId, UUID uomId,
                               String standardCost, String listPrice) throws Exception {
        String body = "{\"companyId\":\"" + COMPANY_ID + "\",\"sku\":\"" + sku + "\",\"name\":\"" + name
                + "\",\"productType\":\"STOCKABLE\",\"categoryId\":\"" + categoryId
                + "\",\"uomId\":\"" + uomId
                + "\",\"standardCost\":" + standardCost + ",\"listPrice\":" + listPrice + "}";
        MvcResult result = mockMvc.perform(post("/api/v1/inventory/products")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
