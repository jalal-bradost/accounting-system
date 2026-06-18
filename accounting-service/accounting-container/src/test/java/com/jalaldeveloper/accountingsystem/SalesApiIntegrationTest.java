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
 * Sales order → outgoing delivery (sync qty_delivered) → customer invoice from SO (tax snapshots) → payment.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SalesApiIntegrationTest {

    private static final UUID COMPANY_ID = DashboardController.DEFAULT_COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void salesOrder_partialDeliver_partialInvoice_payment_updatesLineQuantities() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID vatAcc = accountIdByCode("430011");

        UUID taxId = createFiscalSaleTax("S-VAT-" + UUID.randomUUID().toString().substring(0, 6), vatAcc);

        UUID productId = createProduct("SO-FLOW-" + UUID.randomUUID().toString().substring(0, 6),
                "SO Flow", categoryId, uomId, "10.00", "100.00");

        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "5", "10.00");
        validatePicking(receipt);

        UUID arAccountId = accountIdByCode("430003");
        UUID custId = createCustomer(arAccountId);

        String soBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":3,\"unitPrice\":50,\"discountPercent\":0,\"taxIds\":[\"" + taxId + "\"]}]}";
        MvcResult soCreate = mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(soBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode so = json.readTree(soCreate.getResponse().getContentAsString());
        UUID soId = UUID.fromString(so.get("id").asText());
        assertThat(so.get("lines").get(0).get("qtyDelivered").decimalValue()).isEqualByComparingTo(BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/sales/orders/" + soId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        MvcResult soAfter = mockMvc.perform(get("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode soNode = json.readTree(soAfter.getResponse().getContentAsString());
        UUID pickingId = UUID.fromString(soNode.get("deliveryPickingIds").get(0).asText());

        mockMvc.perform(post("/api/v1/inventory/pickings/" + pickingId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/inventory/pickings/" + pickingId + "/assign")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        UUID moveId = firstMoveId(pickingId);
        validatePickingWithPicks(pickingId, moveId, "1", true);

        MvcResult soDelivered = mockMvc.perform(get("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode line = json.readTree(soDelivered.getResponse().getContentAsString()).get("lines").get(0);
        assertThat(line.get("qtyDelivered").decimalValue()).isEqualByComparingTo(new BigDecimal("1.0000"));

        String invCmd = "{\"salesOrderId\":\"" + soId + "\",\"invoiceDate\":\"2026-05-04\",\"dueDate\":\"2026-06-04\"}";
        MvcResult invRes = mockMvc.perform(post("/api/v1/sales/customer-invoices/from-order")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invCmd))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode inv = json.readTree(invRes.getResponse().getContentAsString());
        UUID invoiceId = UUID.fromString(inv.get("id").asText());
        assertThat(inv.get("lines").get(0).get("qty").decimalValue()).isEqualByComparingTo(new BigDecimal("1.0000"));
        assertThat(inv.get("lines").get(0).get("taxSnapshots")).isNotEmpty();

        mockMvc.perform(post("/api/v1/accounting/customer-invoices/" + invoiceId + "/post")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        MvcResult soInvoiced = mockMvc.perform(get("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json.readTree(soInvoiced.getResponse().getContentAsString()).get("lines").get(0)
                .get("qtyInvoiced").decimalValue()).isEqualByComparingTo(new BigDecimal("1.0000"));

        UUID cashJournalId = journalIdByType("CASH");
        MvcResult postedInv = mockMvc.perform(get("/api/v1/accounting/customer-invoices/" + invoiceId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode posted = json.readTree(postedInv.getResponse().getContentAsString());
        UUID jeId = UUID.fromString(posted.get("journalEntryId").asText());
        BigDecimal arTotal = BigDecimal.ZERO;
        MvcResult jeResult = mockMvc.perform(get("/api/v1/journal-entries/" + jeId))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode it : json.readTree(jeResult.getResponse().getContentAsString()).get("items")) {
            if (it.get("accountId").asText().equals(arAccountId.toString())) {
                arTotal = arTotal.add(it.get("debit").decimalValue());
            }
        }
        assertThat(arTotal).isGreaterThan(BigDecimal.ZERO);

        String payBody = "{\"customerInvoiceId\":\"" + invoiceId + "\",\"paymentJournalId\":\"" + cashJournalId
                + "\",\"paymentDate\":\"2026-05-04\",\"amount\":" + arTotal.toPlainString()
                + ",\"currencyCode\":\"USD\",\"reference\":\"SO-PAY\"}";
        mockMvc.perform(post("/api/v1/accounting/customer-invoices/payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk());

        // Ensure list price path without explicit unit price still works via pricelist-less SO
        String so2 = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"LP\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":1,\"taxIds\":[]}]}";
        mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(so2))
                .andExpect(status().isOk());
    }

    @Test
    void duplicate_draft_customer_invoice_from_same_so_is_rejected() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID vatAcc = accountIdByCode("430011");
        UUID taxId = createFiscalSaleTax("S-DUP-" + UUID.randomUUID().toString().substring(0, 6), vatAcc);
        UUID productId = createProduct("SO-DUP-" + UUID.randomUUID().toString().substring(0, 6),
                "Duplicate invoice test", categoryId, uomId, "10.00", "100.00");

        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "5", "10.00");
        validatePicking(receipt);

        UUID arAccountId = accountIdByCode("430003");
        UUID custId = createCustomer(arAccountId);

        String soBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":2,\"unitPrice\":50,\"discountPercent\":0,\"taxIds\":[\"" + taxId + "\"]}]}";
        JsonNode so = json.readTree(mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(soBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID soId = UUID.fromString(so.get("id").asText());

        mockMvc.perform(post("/api/v1/sales/orders/" + soId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        JsonNode soJson = json.readTree(mockMvc.perform(get("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID pickingId = UUID.fromString(soJson.get("deliveryPickingIds").get(0).asText());
        mockMvc.perform(post("/api/v1/inventory/pickings/" + pickingId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/inventory/pickings/" + pickingId + "/assign")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        UUID moveId = firstMoveId(pickingId);
        validatePickingWithPicks(pickingId, moveId, "2", false);

        String invCmd = "{\"salesOrderId\":\"" + soId + "\",\"invoiceDate\":\"2026-05-04\",\"dueDate\":\"2026-06-04\"}";
        mockMvc.perform(post("/api/v1/sales/customer-invoices/from-order")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invCmd))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sales/customer-invoices/from-order")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invCmd))
                .andExpect(status().isUnprocessableEntity());

        JsonNode soAfterInv = json.readTree(mockMvc.perform(get("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(soAfterInv.get("canCreateCustomerInvoice").asBoolean()).isFalse();
    }

    @Test
    void customer_payment_cannot_exceed_outstanding_balance() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID vatAcc = accountIdByCode("430011");
        UUID taxId = createFiscalSaleTax("S-PAY-" + UUID.randomUUID().toString().substring(0, 6), vatAcc);
        UUID productId = createProduct("SO-PAY-" + UUID.randomUUID().toString().substring(0, 6),
                "Overpay test", categoryId, uomId, "10.00", "100.00");

        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "5", "10.00");
        validatePicking(receipt);

        UUID arAccountId = accountIdByCode("430003");
        UUID custId = createCustomer(arAccountId);
        UUID cashJournalId = journalIdByType("CASH");

        String soBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":1,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[\"" + taxId + "\"]}]}";
        JsonNode so = json.readTree(mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(soBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID soId = UUID.fromString(so.get("id").asText());

        mockMvc.perform(post("/api/v1/sales/orders/" + soId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        JsonNode soJson = json.readTree(mockMvc.perform(get("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID pickingId = UUID.fromString(soJson.get("deliveryPickingIds").get(0).asText());
        mockMvc.perform(post("/api/v1/inventory/pickings/" + pickingId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/inventory/pickings/" + pickingId + "/assign")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        UUID moveId = firstMoveId(pickingId);
        validatePickingWithPicks(pickingId, moveId, "1", false);

        String invCmd = "{\"salesOrderId\":\"" + soId + "\",\"invoiceDate\":\"2026-05-04\"}";
        JsonNode inv = json.readTree(mockMvc.perform(post("/api/v1/sales/customer-invoices/from-order")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invCmd))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID invoiceId = UUID.fromString(inv.get("id").asText());

        JsonNode posted = json.readTree(mockMvc.perform(post("/api/v1/accounting/customer-invoices/" + invoiceId + "/post")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID journalEntryId = UUID.fromString(posted.get("journalEntryId").asText());

        JsonNode je = json.readTree(mockMvc.perform(get("/api/v1/journal-entries/" + journalEntryId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        BigDecimal arDebit = BigDecimal.ZERO;
        for (JsonNode it : je.get("items")) {
            if (it.get("accountId").asText().equals(arAccountId.toString())) {
                arDebit = arDebit.add(it.get("debit").decimalValue());
            }
        }

        String payBody = "{\"customerInvoiceId\":\"" + invoiceId + "\",\"paymentJournalId\":\"" + cashJournalId
                + "\",\"paymentDate\":\"2026-05-04\",\"amount\":" + arDebit.toPlainString()
                + ",\"currencyCode\":\"USD\",\"reference\":\"PAY-1\"}";
        mockMvc.perform(post("/api/v1/accounting/customer-invoices/payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/accounting/customer-invoices/payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isUnprocessableEntity());
    }

    private UUID createFiscalSaleTax(String name, UUID accountId) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"amountType\":\"PERCENT\",\"amount\":10,\"priceInclude\":false,"
                + "\"scope\":\"SALE\",\"accountId\":\"" + accountId + "\"}";
        MvcResult r = mockMvc.perform(post("/api/v1/purchase/fiscal-taxes")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(json.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID lookupLocationByCode(String code) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/inventory/stock-locations")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode node : json.readTree(result.getResponse().getContentAsString())) {
            if (code.equalsIgnoreCase(node.get("code").asText())) {
                return UUID.fromString(node.get("id").asText());
            }
        }
        throw new AssertionError("location not found: " + code);
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

    private UUID createPicking(UUID warehouseId, String type, UUID sourceLoc, UUID destLoc,
                               UUID productId, UUID uomId, String qty, String unitCost) throws Exception {
        StringBuilder move = new StringBuilder();
        move.append("{\"productId\":\"").append(productId).append("\"")
                .append(",\"uomId\":\"").append(uomId).append("\"")
                .append(",\"demandQuantity\":").append(qty);
        if (unitCost != null) {
            move.append(",\"unitCost\":").append(unitCost);
        }
        move.append("}");
        String body = "{\"companyId\":\"" + COMPANY_ID + "\",\"warehouseId\":\"" + warehouseId
                + "\",\"pickingType\":\"" + type + "\",\"sourceLocationId\":\"" + sourceLoc
                + "\",\"destinationLocationId\":\"" + destLoc + "\",\"moves\":[" + move + "]}";
        MvcResult result = mockMvc.perform(post("/api/v1/inventory/pickings")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void validatePicking(UUID id) throws Exception {
        mockMvc.perform(post("/api/v1/inventory/pickings/" + id + "/validate")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
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

    private UUID createCustomer(UUID receivableAccountId) throws Exception {
        String body = "{\"kind\":\"COMPANY\",\"displayName\":\"SO Cust " + UUID.randomUUID().toString().substring(0, 6)
                + "\",\"customer\":true,\"vendor\":false,\"receivableAccountId\":\"" + receivableAccountId
                + "\",\"currencyCode\":\"USD\"}";
        MvcResult r = mockMvc.perform(post("/api/v1/contacts/partners")
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
        for (JsonNode a : json.readTree(r.getResponse().getContentAsString())) {
            if (code.equals(a.get("code").asText())) {
                return UUID.fromString(a.get("id").asText());
            }
        }
        throw new AssertionError("account not found: " + code);
    }

    private UUID journalIdByType(String type) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/journals").param("companyId", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode j : json.readTree(r.getResponse().getContentAsString())) {
            if (j.has("journalType") && type.equalsIgnoreCase(j.get("journalType").asText())) {
                return UUID.fromString(j.get("id").asText());
            }
        }
        throw new AssertionError("journal not found: " + type);
    }
}
