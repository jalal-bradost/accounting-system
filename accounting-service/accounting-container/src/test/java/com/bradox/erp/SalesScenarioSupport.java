package com.bradox.erp;

import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared setup for the sales correction scenario tests: builds orders, deliveries, invoices and
 * returns through the REST API, and checks the consistency report for an order.
 */
abstract class SalesScenarioSupport {

    protected static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper json;

    protected UUID warehouse;
    protected UUID uomId;
    protected UUID customer;

    @BeforeEach
    void setUp() throws Exception {
        warehouse = lookupId("/api/v1/inventory/warehouses", "code", "WH");
        uomId = lookupUom("Unit");
        customer = createCustomer();
    }

    // ---------------------------------------------------------------- helpers

    protected record Line(String id, UUID productId, int qty, int price, int discountPercent, boolean gift) {}

    protected static Line line(String id, UUID productId, int qty, int price) {
        return new Line(id, productId, qty, price, 0, false);
    }

    protected static Line lineWithDiscount(String id, UUID productId, int qty, int price, int discountPercent) {
        return new Line(id, productId, qty, price, discountPercent, false);
    }

    protected String orderBody(UUID customerId, Long rowVersion, Line... lines) {
        List<String> parts = new ArrayList<>();
        for (Line l : lines) {
            parts.add("{" + (l.id() != null ? "\"id\":\"" + l.id() + "\"," : "")
                    + "\"productId\":\"" + l.productId() + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                    + "\",\"qtyOrdered\":" + l.qty() + ",\"unitPrice\":" + l.price()
                    + ",\"discountPercent\":" + l.discountPercent() + (l.gift() ? ",\"isGift\":true" : "")
                    + ",\"taxIds\":[]}");
        }
        return "{\"customerPartnerId\":\"" + customerId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\"" + (rowVersion != null ? ",\"rowVersion\":" + rowVersion : "")
                + ",\"lines\":[" + String.join(",", parts) + "]}";
    }

    protected JsonNode createOrder(Line... lines) throws Exception {
        return call(post("/api/v1/sales/orders").contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(customer, null, lines))).andExpect(status().isOk()).json();
    }

    protected JsonNode confirm(JsonNode so) throws Exception {
        return call(post(orderUrl(so) + "/confirm")).andExpect(status().isOk()).json();
    }

    protected Result amend(JsonNode so, Line... lines) throws Exception {
        return call(put(orderUrl(so)).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(customer, so.get("rowVersion").asLong(), lines)));
    }

    protected JsonNode deliverAll(JsonNode so) throws Exception {
        for (UUID picking : openDeliveries(so)) {
            validate(picking, null, null, false);
        }
        return reload(so);
    }

    protected UUID createInvoiceFromOrder(JsonNode so) throws Exception {
        return call(post("/api/v1/sales/customer-invoices/from-order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"salesOrderId\":\"" + so.get("id").asText()
                        + "\",\"invoiceDate\":\"2026-05-04\",\"dueDate\":\"2026-06-04\"}"))
                .andExpect(status().isOk()).id();
    }

    protected JsonNode invoiceAll(JsonNode so) throws Exception {
        UUID invoice = createInvoiceFromOrder(so);
        call(post("/api/v1/accounting/customer-invoices/" + invoice + "/post")).andExpect(status().isOk());
        return reload(so);
    }

    protected Result returnPicking(UUID pickingId) throws Exception {
        return returnPicking(pickingId, true);
    }

    /** Inventory-level return of a picking; toRefund=false means the goods will be replaced. */
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

    protected JsonNode reload(JsonNode so) throws Exception {
        return call(get(orderUrl(so))).andExpect(status().isOk()).json();
    }

    protected List<UUID> openDeliveries(JsonNode so) throws Exception {
        List<UUID> open = new ArrayList<>();
        for (JsonNode id : reload(so).get("deliveryPickingIds")) {
            String state = picking(UUID.fromString(id.asText())).get("state").asText();
            if (!"DONE".equals(state) && !"CANCELLED".equals(state)) {
                open.add(UUID.fromString(id.asText()));
            }
        }
        return open;
    }

    protected BigDecimal openDemand(JsonNode so, String lineId) throws Exception {
        BigDecimal total = BigDecimal.ZERO;
        for (UUID pickingId : openDeliveries(so)) {
            for (JsonNode m : picking(pickingId).get("moves")) {
                if (lineId.equals(m.path("salesOrderLineId").asText())
                        && !"CANCELLED".equals(m.get("state").asText()) && !"DONE".equals(m.get("state").asText())) {
                    total = total.add(m.get("demandQuantity").decimalValue());
                }
            }
        }
        return total;
    }

    protected JsonNode picking(UUID id) throws Exception {
        return call(get("/api/v1/inventory/pickings/" + id)).andExpect(status().isOk()).json();
    }

    protected UUID firstMoveId(UUID pickingId) throws Exception {
        return UUID.fromString(picking(pickingId).get("moves").get(0).get("id").asText());
    }

    protected void assertConsistent(JsonNode so) throws Exception {
        JsonNode issues = call(get("/api/v1/sales/consistency").param("salesOrderId", so.get("id").asText()))
                .andExpect(status().isOk()).json();
        assertThat(issues).as("consistency issues for %s: %s", so.get("name").asText(), issues).isEmpty();
    }

    protected static String lineId(JsonNode so, int index) {
        return so.get("lines").get(index).get("id").asText();
    }

    protected static String orderUrl(JsonNode so) {
        return "/api/v1/sales/orders/" + so.get("id").asText();
    }

    protected UUID stockedProduct(int onHand) throws Exception {
        UUID category = lookupId("/api/v1/inventory/product-categories", "name", "All");
        String sku = "P1-" + UUID.randomUUID().toString().substring(0, 8);
        UUID product = call(post("/api/v1/inventory/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\":\"" + COMPANY_ID + "\",\"sku\":\"" + sku + "\",\"name\":\"" + sku
                        + "\",\"productType\":\"STOCKABLE\",\"categoryId\":\"" + category + "\",\"uomId\":\"" + uomId
                        + "\",\"standardCost\":10,\"listPrice\":100}"))
                .andExpect(status().isOk()).id();
        UUID stock = lookupId("/api/v1/inventory/stock-locations", "code", "WH/STOCK");
        UUID supplier = lookupId("/api/v1/inventory/stock-locations", "code", "VIRT/SUPPLIERS");
        UUID receipt = call(post("/api/v1/inventory/pickings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\":\"" + COMPANY_ID + "\",\"warehouseId\":\"" + warehouse
                        + "\",\"pickingType\":\"INCOMING\",\"sourceLocationId\":\"" + supplier
                        + "\",\"destinationLocationId\":\"" + stock + "\",\"moves\":[{\"productId\":\"" + product
                        + "\",\"uomId\":\"" + uomId + "\",\"demandQuantity\":" + onHand + ",\"unitCost\":10}]}"))
                .andExpect(status().isOk()).id();
        validate(receipt, null, null, false);
        return product;
    }

    protected UUID serviceProduct() throws Exception {
        UUID category = lookupId("/api/v1/inventory/product-categories", "name", "All");
        String sku = "S1-" + UUID.randomUUID().toString().substring(0, 8);
        return call(post("/api/v1/inventory/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\":\"" + COMPANY_ID + "\",\"sku\":\"" + sku + "\",\"name\":\"" + sku
                        + "\",\"productType\":\"SERVICE\",\"categoryId\":\"" + category + "\",\"uomId\":\"" + uomId
                        + "\",\"standardCost\":0,\"listPrice\":100}"))
                .andExpect(status().isOk()).id();
    }

    protected UUID createCustomer() throws Exception {
        UUID ar = null;
        for (JsonNode a : call(get("/api/v1/accounts").param("companyId", COMPANY_ID.toString())).json()) {
            if ("430003".equals(a.get("code").asText())) {
                ar = UUID.fromString(a.get("id").asText());
            }
        }
        return call(post("/api/v1/contacts/partners").contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"COMPANY\",\"displayName\":\"P1 Cust " + UUID.randomUUID().toString().substring(0, 6)
                        + "\",\"customer\":true,\"vendor\":false,\"receivableAccountId\":\"" + ar
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

    protected Result returnGoods(JsonNode so, boolean refund, boolean damaged, String lineId, String qty)
            throws Exception {
        return call(post(orderUrl(so) + "/returns").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refund\":" + refund + ",\"damaged\":" + damaged + ",\"reason\":\"test\","
                        + "\"lines\":[{\"salesOrderLineId\":\"" + lineId + "\",\"qty\":" + qty + "}]}"));
    }

    protected List<UUID> postedInvoices(JsonNode so) throws Exception {
        List<UUID> ids = new ArrayList<>();
        for (JsonNode inv : call(get("/api/v1/accounting/customer-invoices")).json()) {
            if (so.get("id").asText().equals(inv.path("salesOrderId").asText())
                    && "POSTED".equals(inv.get("state").asText())
                    && !"CREDIT_NOTE".equals(inv.path("moveType").asText())) {
                ids.add(UUID.fromString(inv.get("id").asText()));
            }
        }
        return ids;
    }

    protected JsonNode creditNotes(UUID invoiceId) throws Exception {
        return call(get("/api/v1/accounting/customer-invoices/" + invoiceId + "/credit-notes"))
                .andExpect(status().isOk()).json();
    }

    protected BigDecimal onHand(UUID product) throws Exception {
        return call(get("/api/v1/inventory/on-hand/" + product)).andExpect(status().isOk()).json()
                .get("totalOnHand").decimalValue();
    }

    protected void receive(UUID product, int qty, int unitCost) throws Exception {
        UUID stock = lookupId("/api/v1/inventory/stock-locations", "code", "WH/STOCK");
        UUID supplier = lookupId("/api/v1/inventory/stock-locations", "code", "VIRT/SUPPLIERS");
        UUID receipt = call(post("/api/v1/inventory/pickings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\":\"" + COMPANY_ID + "\",\"warehouseId\":\"" + warehouse
                        + "\",\"pickingType\":\"INCOMING\",\"sourceLocationId\":\"" + supplier
                        + "\",\"destinationLocationId\":\"" + stock + "\",\"moves\":[{\"productId\":\"" + product
                        + "\",\"uomId\":\"" + uomId + "\",\"demandQuantity\":" + qty + ",\"unitCost\":" + unitCost + "}]}"))
                .andExpect(status().isOk()).id();
        validate(receipt, null, null, false);
    }

    /** Id of the company's cash journal, used to register payments and refunds. */
    protected UUID cashJournal() throws Exception {
        for (JsonNode j : call(get("/api/v1/journals").param("companyId", COMPANY_ID.toString())).json()) {
            if (j.has("journalType") && "CASH".equalsIgnoreCase(j.get("journalType").asText())) {
                return UUID.fromString(j.get("id").asText());
            }
        }
        throw new AssertionError("no cash journal");
    }

    /** Registers a payment against a posted invoice, or a refund against a posted credit note. */
    protected Result pay(UUID documentId, String amount) throws Exception {
        return call(post("/api/v1/accounting/customer-invoices/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerInvoiceId\":\"" + documentId + "\",\"paymentJournalId\":\"" + cashJournal()
                        + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":" + amount
                        + ",\"currencyCode\":\"USD\",\"reference\":\"TEST\"}"));
    }

    protected JsonNode invoiceDoc(UUID id) throws Exception {
        return call(get("/api/v1/accounting/customer-invoices/" + id)).andExpect(status().isOk()).json();
    }

    protected Result call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return new Result(mockMvc.perform(request.header("X-Company-Id", COMPANY_ID.toString())));
    }

    /** Same as {@link #call} but acting as a signed-in user, for modules that resolve the current user (Timesheet). */
    protected Result callAs(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, UUID userId)
            throws Exception {
        return new Result(mockMvc.perform(request.header("X-Company-Id", COMPANY_ID.toString()).header("X-User-Id", userId.toString())));
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
