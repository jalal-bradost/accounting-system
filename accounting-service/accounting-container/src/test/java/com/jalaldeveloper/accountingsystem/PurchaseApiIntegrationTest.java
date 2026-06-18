package com.jalaldeveloper.accountingsystem;

import com.jalaldeveloper.accountingsystem.web.DashboardController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end purchase flow: vendor, fiscal tax, PO, confirm (incoming picking), receive via
 * purchase receipt validate, vendor bill from PO, post, register payment with AP reconciliation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseApiIntegrationTest {

    private static final UUID COMPANY_ID = DashboardController.DEFAULT_COMPANY_ID;

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

        mockMvc.perform(post("/api/v1/purchase/orders/" + poId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        MvcResult po2 = mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode poJson = json.readTree(po2.getResponse().getContentAsString());
        assertThat(poJson.get("receiptPickingIds").isArray()).isTrue();
        assertThat(poJson.get("receiptPickingIds").size()).isPositive();
        UUID pickingId = UUID.fromString(poJson.get("receiptPickingIds").get(0).asText());

        mockMvc.perform(post("/api/v1/purchase/receipts/" + pickingId + "/validate")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        String billBody = "{\"purchaseOrderId\":\"" + poId + "\",\"billDate\":\"2026-05-04\"}";
        MvcResult billResult = mockMvc.perform(post("/api/v1/purchase/vendor-bills/from-po")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode bill = json.readTree(billResult.getResponse().getContentAsString());
        UUID billId = UUID.fromString(bill.get("id").asText());

        MvcResult getBillRes = mockMvc.perform(get("/api/v1/purchase/vendor-bills/" + billId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode billDetail = json.readTree(getBillRes.getResponse().getContentAsString());
        assertThat(billDetail.get("lines").isArray()).isTrue();
        assertThat(billDetail.get("lines").size()).isPositive();

        MvcResult postBillRes = mockMvc.perform(post("/api/v1/purchase/vendor-bills/" + billId + "/post")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode postedBill = json.readTree(postBillRes.getResponse().getContentAsString());
        UUID journalEntryId = UUID.fromString(postedBill.get("journalEntryId").asText());

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
                + "\",\"paymentDate\":\"2026-05-04\",\"amount\":" + apCredit.toPlainString()
                + ",\"currencyCode\":\"USD\",\"reference\":\"TEST-PAY\"}";
        MvcResult payRes = mockMvc.perform(post("/api/v1/purchase/vendor-payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pay = json.readTree(payRes.getResponse().getContentAsString());
        assertThat(pay.get("reconciliationId").asText()).isNotBlank();

        MvcResult billsListRes = mockMvc.perform(get("/api/v1/purchase/vendor-bills")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode billsArr = json.readTree(billsListRes.getResponse().getContentAsString());
        assertThat(billsArr.isArray()).isTrue();
        boolean billInList = false;
        for (JsonNode b : billsArr) {
            if (billId.toString().equals(b.get("id").asText())) {
                billInList = true;
                assertThat(b.get("state").asText()).isEqualTo("POSTED");
                assertThat(b.get("purchaseOrderId").asText()).isEqualTo(poId.toString());
                break;
            }
        }
        assertThat(billInList).isTrue();

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

        JsonNode poAfter = json.readTree(mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(poAfter.get("lines").get(0).get("qtyReceived").decimalValue())
                .isEqualByComparingTo(new BigDecimal("3"));
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

        mockMvc.perform(post("/api/v1/purchase/orders/" + poId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        JsonNode poJson = json.readTree(mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID pickingId = UUID.fromString(poJson.get("receiptPickingIds").get(0).asText());
        mockMvc.perform(post("/api/v1/purchase/receipts/" + pickingId + "/validate")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        String billBody = "{\"purchaseOrderId\":\"" + poId + "\",\"billDate\":\"2026-05-04\"}";
        mockMvc.perform(post("/api/v1/purchase/vendor-bills/from-po")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/purchase/vendor-bills/from-po")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billBody))
                .andExpect(status().isUnprocessableEntity());

        JsonNode poAfterBill = json.readTree(mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(poAfterBill.get("canCreateVendorBill").asBoolean()).isFalse();
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

        mockMvc.perform(post("/api/v1/purchase/orders/" + poId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        JsonNode poJson = json.readTree(mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID pickingId = UUID.fromString(poJson.get("receiptPickingIds").get(0).asText());
        mockMvc.perform(post("/api/v1/purchase/receipts/" + pickingId + "/validate")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        String billBody = "{\"purchaseOrderId\":\"" + poId + "\",\"billDate\":\"2026-05-04\"}";
        JsonNode bill = json.readTree(mockMvc.perform(post("/api/v1/purchase/vendor-bills/from-po")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID billId = UUID.fromString(bill.get("id").asText());

        JsonNode postedBill = json.readTree(mockMvc.perform(post("/api/v1/purchase/vendor-bills/" + billId + "/post")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID journalEntryId = UUID.fromString(postedBill.get("journalEntryId").asText());

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
                + "\",\"paymentDate\":\"2026-05-04\",\"amount\":" + apCredit.toPlainString()
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
     * Validates through inventory (not purchase receipt URL) — PO line qty_received must still update.
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
        UUID poId = UUID.fromString(po.get("id").asText());

        mockMvc.perform(post("/api/v1/purchase/orders/" + poId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        JsonNode poJson = json.readTree(mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID pickingId = UUID.fromString(poJson.get("receiptPickingIds").get(0).asText());

        mockMvc.perform(post("/api/v1/inventory/pickings/" + pickingId + "/validate")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        JsonNode poAfter = json.readTree(mockMvc.perform(get("/api/v1/purchase/orders/" + poId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(poAfter.get("lines").get(0).get("qtyReceived").decimalValue())
                .isEqualByComparingTo(new BigDecimal("4"));
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
