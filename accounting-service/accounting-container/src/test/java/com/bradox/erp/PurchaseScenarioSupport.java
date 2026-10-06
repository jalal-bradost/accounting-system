package com.bradox.erp;

import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared setup for the purchase correction scenario tests: builds orders, receipts, bills, returns
 * and payments through the REST API, and checks the consistency report for an order.
 */
abstract class PurchaseScenarioSupport {

    protected static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper json;

    protected UUID warehouse;
    protected UUID uomId;
    protected UUID vendor;

    @BeforeEach
    void setUpPurchase() throws Exception {
        warehouse = lookupId("/api/v1/inventory/warehouses", "code", "WH");
        uomId = lookupUom("Unit");
        vendor = createVendor();
    }

    protected record Line(String id, UUID productId, int qty, int price, int discountPercent) {}

    protected static Line line(String id, UUID productId, int qty, int price) {
        return new Line(id, productId, qty, price, 0);
    }

    protected static Line lineWithDiscount(String id, UUID productId, int qty, int price, int discountPercent) {
        return new Line(id, productId, qty, price, discountPercent);
    }

    protected String orderBody(UUID vendorId, Long rowVersion, Line... lines) {
        List<String> parts = new ArrayList<>();
        for (Line l : lines) {
            parts.add("{" + (l.id() != null ? "\"id\":\"" + l.id() + "\"," : "")
                    + "\"productId\":\"" + l.productId() + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                    + "\",\"qtyOrdered\":" + l.qty() + ",\"unitPrice\":" + l.price()
                    + ",\"discountPercent\":" + l.discountPercent() + ",\"taxIds\":[]}");
        }
        return "{\"vendorPartnerId\":\"" + vendorId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\"" + (rowVersion != null ? ",\"rowVersion\":" + rowVersion : "")
                + ",\"lines\":[" + String.join(",", parts) + "]}";
    }

    // ---------------------------------------------------------------- order flow

    protected JsonNode createOrder(Line... lines) throws Exception {
        return call(post("/api/v1/purchase/orders").contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(vendor, null, lines))).andExpect(status().isOk()).json();
    }

    protected JsonNode confirm(JsonNode po) throws Exception {
        return call(post(orderUrl(po) + "/confirm")).andExpect(status().isOk()).json();
    }

    protected Result amend(JsonNode po, Line... lines) throws Exception {
        return call(put(orderUrl(po)).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(vendor, po.get("rowVersion").asLong(), lines)));
    }

    protected JsonNode receiveAll(JsonNode po) throws Exception {
        for (UUID picking : openReceipts(po)) {
            validate(picking, null, null, false);
        }
        return reload(po);
    }

    protected UUID createBillFromOrder(JsonNode po) throws Exception {
        return call(post("/api/v1/purchase/vendor-bills/from-po").contentType(MediaType.APPLICATION_JSON)
                .content("{\"purchaseOrderId\":\"" + po.get("id").asText()
                        + "\",\"billDate\":\"2026-05-04\",\"dueDate\":\"2026-06-04\"}"))
                .andExpect(status().isOk()).id();
    }

    protected JsonNode billAll(JsonNode po) throws Exception {
        UUID bill = createBillFromOrder(po);
        call(post("/api/v1/purchase/vendor-bills/" + bill + "/post")).andExpect(status().isOk());
        return reload(po);
    }

    protected Result returnPicking(UUID pickingId, boolean toRefund) throws Exception {
        return call(post("/api/v1/inventory/pickings/" + pickingId + "/return")
                .contentType(MediaType.APPLICATION_JSON).content("{\"toRefund\":" + toRefund + "}"));
    }

    protected void validate(UUID pickingId, UUID moveId, String picked, boolean backorder) throws Exception {
        String body = moveId == null ? "{}"
                : "{\"createBackorder\":" + backorder + ",\"picks\":[{\"moveId\":\"" + moveId
                        + "\",\"pickedQuantity\":" + picked + "}]}";
        call(post("/api/v1/inventory/pickings/" + pickingId + "/validate").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isOk());
    }

    protected JsonNode reload(JsonNode po) throws Exception {
        return call(get(orderUrl(po))).andExpect(status().isOk()).json();
    }

    // ---------------------------------------------------------------- reading state

    protected List<UUID> openReceipts(JsonNode po) throws Exception {
        List<UUID> open = new ArrayList<>();
        for (JsonNode id : reload(po).get("receiptPickingIds")) {
            String state = picking(UUID.fromString(id.asText())).get("state").asText();
            if (!"DONE".equals(state) && !"CANCELLED".equals(state)) {
                open.add(UUID.fromString(id.asText()));
            }
        }
        return open;
    }

    protected BigDecimal openDemand(JsonNode po, String lineId) throws Exception {
        BigDecimal total = BigDecimal.ZERO;
        for (UUID pickingId : openReceipts(po)) {
            for (JsonNode m : picking(pickingId).get("moves")) {
                if (lineId.equals(m.path("purchaseOrderLineId").asText())
                        && !"CANCELLED".equals(m.get("state").asText()) && !"DONE".equals(m.get("state").asText())) {
                    total = total.add(m.get("demandQuantity").decimalValue());
                }
            }
        }
        return total;
    }

    protected BigDecimal openCost(JsonNode po, String lineId) throws Exception {
        for (UUID pickingId : openReceipts(po)) {
            for (JsonNode m : picking(pickingId).get("moves")) {
                if (lineId.equals(m.path("purchaseOrderLineId").asText())
                        && !"CANCELLED".equals(m.get("state").asText()) && !"DONE".equals(m.get("state").asText())) {
                    return m.get("unitCost").decimalValue();
                }
            }
        }
        return BigDecimal.ZERO;
    }

    protected JsonNode picking(UUID id) throws Exception {
        return call(get("/api/v1/inventory/pickings/" + id)).andExpect(status().isOk()).json();
    }

    protected UUID firstMoveId(UUID pickingId) throws Exception {
        return UUID.fromString(picking(pickingId).get("moves").get(0).get("id").asText());
    }

    protected List<UUID> postedBills(JsonNode po) throws Exception {
        List<UUID> ids = new ArrayList<>();
        for (JsonNode b : call(get("/api/v1/purchase/vendor-bills")).json()) {
            if (po.get("id").asText().equals(b.path("purchaseOrderId").asText())
                    && "POSTED".equals(b.get("state").asText())
                    && !"CREDIT_NOTE".equals(b.path("moveType").asText())
                    && !"DEBIT_NOTE".equals(b.path("moveType").asText())) {
                ids.add(UUID.fromString(b.get("id").asText()));
            }
        }
        return ids;
    }

    protected JsonNode creditNotes(UUID billId) throws Exception {
        return call(get("/api/v1/accounting/vendor-bills/" + billId + "/credit-notes"))
                .andExpect(status().isOk()).json();
    }

    protected JsonNode billDoc(UUID id) throws Exception {
        return call(get("/api/v1/purchase/vendor-bills/" + id)).andExpect(status().isOk()).json();
    }

    /** Posted bills minus posted credit notes of the order, tax included. */
    protected BigDecimal netBilled(JsonNode po) throws Exception {
        BigDecimal net = BigDecimal.ZERO;
        for (JsonNode b : call(get("/api/v1/purchase/vendor-bills")).json()) {
            if (!po.get("id").asText().equals(b.path("purchaseOrderId").asText())
                    || !"POSTED".equals(b.get("state").asText())) {
                continue;
            }
            String type = b.path("moveType").asText();
            if ("DEBIT_NOTE".equals(type)) {
                continue;
            }
            BigDecimal total = billDoc(UUID.fromString(b.get("id").asText())).get("amountTotal").decimalValue();
            net = "CREDIT_NOTE".equals(type) ? net.subtract(total) : net.add(total);
        }
        return net;
    }

    protected BigDecimal onHand(UUID product) throws Exception {
        return call(get("/api/v1/inventory/on-hand/" + product)).andExpect(status().isOk()).json()
                .get("totalOnHand").decimalValue();
    }

    /** Net debit on an account (by code) across the whole ledger. */
    protected BigDecimal accountBalance(String code) throws Exception {
        UUID account = null;
        for (JsonNode a : call(get("/api/v1/accounts").param("companyId", COMPANY_ID.toString())).json()) {
            if (code.equals(a.get("code").asText())) {
                account = UUID.fromString(a.get("id").asText());
            }
        }
        assertThat(account).as("account " + code).isNotNull();
        BigDecimal net = BigDecimal.ZERO;
        for (JsonNode l : call(get("/api/v1/companies/" + COMPANY_ID + "/general-ledger")
                .param("from", "2000-01-01").param("to", "2100-12-31").param("accountId", account.toString()))
                .andExpect(status().isOk()).json().get("lines")) {
            net = net.add(l.get("debit").decimalValue()).subtract(l.get("credit").decimalValue());
        }
        return net;
    }

    protected void assertConsistent(JsonNode po) throws Exception {
        JsonNode issues = call(get("/api/v1/purchase/consistency").param("purchaseOrderId", po.get("id").asText()))
                .andExpect(status().isOk()).json();
        assertThat(issues).as("consistency issues for %s: %s", po.get("name").asText(), issues).isEmpty();
    }

    protected static String lineId(JsonNode po, int index) {
        return po.get("lines").get(index).get("id").asText();
    }

    protected static String orderUrl(JsonNode po) {
        return "/api/v1/purchase/orders/" + po.get("id").asText();
    }

    // ---------------------------------------------------------------- payments

    protected UUID bankJournal() throws Exception {
        for (JsonNode j : call(get("/api/v1/journals").param("companyId", COMPANY_ID.toString())).json()) {
            if (j.has("journalType") && "BANK".equalsIgnoreCase(j.get("journalType").asText())) {
                return UUID.fromString(j.get("id").asText());
            }
        }
        throw new AssertionError("no bank journal");
    }

    /** Registers a payment to a vendor against a posted bill, or a refund against a vendor credit note. */
    protected Result pay(UUID documentId, String amount) throws Exception {
        return call(post("/api/v1/purchase/vendor-payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"vendorBillId\":\"" + documentId + "\",\"bankJournalId\":\"" + bankJournal()
                        + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":" + amount
                        + ",\"currencyCode\":\"USD\",\"reference\":\"TEST\"}"));
    }

    // ---------------------------------------------------------------- reference data

    protected UUID product() throws Exception {
        return createProduct("STOCKABLE");
    }

    protected UUID serviceProduct() throws Exception {
        return createProduct("SERVICE");
    }

    private UUID createProduct(String type) throws Exception {
        UUID category = lookupId("/api/v1/inventory/product-categories", "name", "All");
        String sku = "PU-" + UUID.randomUUID().toString().substring(0, 8);
        return call(post("/api/v1/inventory/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\":\"" + COMPANY_ID + "\",\"sku\":\"" + sku + "\",\"name\":\"" + sku
                        + "\",\"productType\":\"" + type + "\",\"categoryId\":\"" + category + "\",\"uomId\":\"" + uomId
                        + "\",\"standardCost\":10,\"listPrice\":100}"))
                .andExpect(status().isOk()).id();
    }

    protected UUID createVendor() throws Exception {
        UUID ap = null;
        for (JsonNode a : call(get("/api/v1/accounts").param("companyId", COMPANY_ID.toString())).json()) {
            if ("430004".equals(a.get("code").asText())) {
                ap = UUID.fromString(a.get("id").asText());
            }
        }
        return call(post("/api/v1/contacts/partners").contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"COMPANY\",\"displayName\":\"PU Vendor " + UUID.randomUUID().toString().substring(0, 6)
                        + "\",\"customer\":false,\"vendor\":true,\"payableAccountId\":\"" + ap
                        + "\",\"currencyCode\":\"USD\"}"))
                .andExpect(status().isOk()).id();
    }

    protected UUID lookupId(String url, String field, String value) throws Exception {
        for (JsonNode n : call(get(url)).andExpect(status().isOk()).json()) {
            if (value.equalsIgnoreCase(n.get(field).asText())) {
                return UUID.fromString(n.get("id").asText());
            }
        }
        throw new AssertionError(url + " has no " + field + "=" + value);
    }

    protected UUID lookupUom(String name) throws Exception {
        for (JsonNode cat : call(get("/api/v1/inventory/uom-categories")).json()) {
            for (JsonNode uom : call(get("/api/v1/inventory/uom-categories/" + cat.get("id").asText() + "/uoms")).json()) {
                if (name.equalsIgnoreCase(uom.get("name").asText())) {
                    return UUID.fromString(uom.get("id").asText());
                }
            }
        }
        throw new AssertionError("uom not found: " + name);
    }

    protected Result call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return new Result(mockMvc.perform(request.header("X-Company-Id", COMPANY_ID.toString())));
    }

    /** Thin wrapper so tests read as call(...).andExpect(...).json(). */
    protected final class Result {
        private final ResultActions actions;

        private Result(ResultActions actions) {
            this.actions = actions;
        }

        Result andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            actions.andExpect(matcher);
            return this;
        }

        /** 422 whose message contains the given text, so the test proves which rule refused. */
        Result refusedWith(String fragment) throws Exception {
            actions.andExpect(status().isUnprocessableEntity());
            String message = json().path("message").asText();
            assertThat(message).as("error message").containsIgnoringCase(fragment);
            return this;
        }

        JsonNode json() throws Exception {
            return json.readTree(actions.andReturn().getResponse().getContentAsString());
        }

        UUID id() throws Exception {
            return UUID.fromString(json().get("id").asText());
        }
    }
}
