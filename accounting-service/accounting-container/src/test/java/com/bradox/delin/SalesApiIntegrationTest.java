package com.bradox.delin;

import com.bradox.delin.platform.bootstrap.PlatformRbacSeeder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sales order → outgoing delivery (sync qty_delivered) → customer invoice from SO (tax snapshots) → payment.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SalesApiIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

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
        assertThat(so.get("state").asText()).isEqualTo("DRAFT");
        so = confirmSalesOrder(soId);
        assertThat(so.get("state").asText()).isEqualTo("CONFIRMED");
        assertThat(so.get("lines").get(0).get("qtyDelivered").decimalValue())
                .isEqualByComparingTo(new BigDecimal("3"));
        assertThat(so.get("lines").get(0).get("qtyInvoiced").decimalValue())
                .isEqualByComparingTo(new BigDecimal("3"));
        assertThat(so.get("canCreateCustomerInvoice").asBoolean()).isFalse();

        JsonNode invoices = json.readTree(mockMvc.perform(get("/api/v1/accounting/customer-invoices")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID invoiceId = null;
        for (JsonNode inv : invoices) {
            if (inv.has("salesOrderId") && soId.toString().equals(inv.get("salesOrderId").asText())
                    && "POSTED".equals(inv.get("state").asText())) {
                invoiceId = UUID.fromString(inv.get("id").asText());
                break;
            }
        }
        assertThat(invoiceId).isNotNull();

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
                + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":" + arTotal.toPlainString()
                + ",\"currencyCode\":\"USD\",\"reference\":\"SO-PAY\"}";
        mockMvc.perform(post("/api/v1/accounting/customer-invoices/payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk());

        // Second order: confirm after create (needs stock). Seed more stock first.
        UUID receipt2 = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "1", "10.00");
        validatePicking(receipt2);
        String so2 = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"LP\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":1,\"taxIds\":[]}]}";
        JsonNode so2Node = json.readTree(mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(so2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        confirmSalesOrder(UUID.fromString(so2Node.get("id").asText()));
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
        assertThat(so.get("state").asText()).isEqualTo("DRAFT");
        so = confirmSalesOrder(soId);
        assertThat(so.get("canCreateCustomerInvoice").asBoolean()).isFalse();

        String invCmd = "{\"salesOrderId\":\"" + soId + "\",\"invoiceDate\":\"2026-05-04\",\"dueDate\":\"2026-06-04\"}";
        mockMvc.perform(post("/api/v1/sales/customer-invoices/from-order")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invCmd))
                .andExpect(status().isUnprocessableEntity());
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
        confirmSalesOrder(soId);

        JsonNode invoices = json.readTree(mockMvc.perform(get("/api/v1/accounting/customer-invoices")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID invoiceId = null;
        for (JsonNode invNode : invoices) {
            if (invNode.has("salesOrderId") && soId.toString().equals(invNode.get("salesOrderId").asText())
                    && "POSTED".equals(invNode.get("state").asText())) {
                invoiceId = UUID.fromString(invNode.get("id").asText());
                break;
            }
        }
        assertThat(invoiceId).isNotNull();

        JsonNode posted = json.readTree(mockMvc.perform(get("/api/v1/accounting/customer-invoices/" + invoiceId)
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
                + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":" + arDebit.toPlainString()
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

    @Test
    void customer_return_auto_creates_draft_credit_note() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("SO-RET-" + UUID.randomUUID().toString().substring(0, 6),
                "Return CN test", categoryId, uomId, "10.00", "100.00");

        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "5", "10.00");
        validatePicking(receipt);

        UUID arAccountId = accountIdByCode("430003");
        UUID custId = createCustomer(arAccountId);

        String soBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":2,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode so = json.readTree(mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(soBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID soId = UUID.fromString(so.get("id").asText());
        assertThat(so.get("state").asText()).isEqualTo("DRAFT");
        so = confirmSalesOrder(soId);
        assertThat(so.get("state").asText()).isEqualTo("CONFIRMED");
        UUID deliveryPickingId = UUID.fromString(so.get("deliveryPickingIds").get(0).asText());

        JsonNode invoices = json.readTree(mockMvc.perform(get("/api/v1/accounting/customer-invoices")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID invoiceId = null;
        for (JsonNode inv : invoices) {
            if (inv.has("salesOrderId") && soId.toString().equals(inv.get("salesOrderId").asText())
                    && "POSTED".equals(inv.get("state").asText())) {
                invoiceId = UUID.fromString(inv.get("id").asText());
                break;
            }
        }
        assertThat(invoiceId).isNotNull();

        JsonNode returnPicking = json.readTree(mockMvc.perform(post("/api/v1/inventory/pickings/" + deliveryPickingId + "/return")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID returnPickingId = UUID.fromString(returnPicking.get("id").asText());
        mockMvc.perform(post("/api/v1/inventory/pickings/" + returnPickingId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/inventory/pickings/" + returnPickingId + "/assign")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        UUID returnMoveId = firstMoveId(returnPickingId);
        validatePickingWithPicks(returnPickingId, returnMoveId, "1", false);

        JsonNode creditNotes = json.readTree(mockMvc.perform(
                        get("/api/v1/accounting/customer-invoices/" + invoiceId + "/credit-notes")
                                .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(creditNotes).hasSize(1);
        assertThat(creditNotes.get(0).get("state").asText()).isEqualTo("DRAFT");
        assertThat(creditNotes.get(0).get("moveType").asText()).isEqualTo("CREDIT_NOTE");
        assertThat(creditNotes.get(0).get("reversedInvoiceId").asText()).isEqualTo(invoiceId.toString());
    }

    @Test
    void confirmed_so_qty_increase_creates_extra_delivery_and_invoice() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("SO-UP-" + UUID.randomUUID().toString().substring(0, 6),
                "Qty increase SO", categoryId, uomId, "10.00", "100.00");

        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "50", "10.00");
        validatePicking(receipt);

        UUID arAccountId = accountIdByCode("430003");
        UUID custId = createCustomer(arAccountId);

        String soBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":20,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode so = json.readTree(mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(soBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID soId = UUID.fromString(so.get("id").asText());
        so = confirmSalesOrder(soId);
        assertThat(so.get("canCreateReturn").asBoolean()).isTrue();
        String lineId = so.get("lines").get(0).get("id").asText();
        assertThat(so.get("deliveryPickingIds")).hasSize(1);

        String amendBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"id\":\"" + lineId + "\",\"productId\":\"" + productId + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":30,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[]}]}";
        so = json.readTree(mockMvc.perform(put("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(amendBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(so.get("state").asText()).isEqualTo("CONFIRMED");
        assertThat(so.get("lines").get(0).get("qtyOrdered").decimalValue())
                .isEqualByComparingTo(new BigDecimal("30"));
        assertThat(so.get("deliveryPickingIds").size()).isGreaterThanOrEqualTo(2);

        so = deliverAndInvoiceSalesOrder(so);
        assertThat(so.get("lines").get(0).get("qtyDelivered").decimalValue())
                .isEqualByComparingTo(new BigDecimal("30"));

        int postedInvoices = 0;
        for (JsonNode inv : json.readTree(mockMvc.perform(get("/api/v1/accounting/customer-invoices")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())) {
            if (inv.has("salesOrderId") && soId.toString().equals(inv.get("salesOrderId").asText())
                    && "POSTED".equals(inv.get("state").asText())
                    && !"CREDIT_NOTE".equals(inv.path("moveType").asText())) {
                postedInvoices++;
            }
        }
        assertThat(postedInvoices).isGreaterThanOrEqualTo(2);
    }

    @Test
    void confirmed_so_qty_decrease_creates_return_and_credit_note() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("SO-DN-" + UUID.randomUUID().toString().substring(0, 6),
                "Qty decrease SO", categoryId, uomId, "10.00", "100.00");

        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "30", "10.00");
        validatePicking(receipt);

        UUID arAccountId = accountIdByCode("430003");
        UUID custId = createCustomer(arAccountId);

        String soBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":20,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode so = json.readTree(mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(soBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID soId = UUID.fromString(so.get("id").asText());
        so = confirmSalesOrder(soId);
        String lineId = so.get("lines").get(0).get("id").asText();
        UUID invoiceId = null;
        for (JsonNode inv : json.readTree(mockMvc.perform(get("/api/v1/accounting/customer-invoices")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())) {
            if (inv.has("salesOrderId") && soId.toString().equals(inv.get("salesOrderId").asText())
                    && "POSTED".equals(inv.get("state").asText())
                    && !"CREDIT_NOTE".equals(inv.path("moveType").asText())) {
                invoiceId = UUID.fromString(inv.get("id").asText());
                break;
            }
        }
        assertThat(invoiceId).isNotNull();

        String amendBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"id\":\"" + lineId + "\",\"productId\":\"" + productId + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":10,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[]}]}";
        so = json.readTree(mockMvc.perform(put("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(amendBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(so.get("state").asText()).isEqualTo("CONFIRMED");
        assertThat(so.get("lines").get(0).get("qtyOrdered").decimalValue())
                .isEqualByComparingTo(new BigDecimal("10"));
        assertThat(so.get("returnPickingIds").size()).isGreaterThanOrEqualTo(1);

        so = validateOpenReturnPickings(so);
        assertThat(so.get("lines").get(0).get("qtyDelivered").decimalValue())
                .isEqualByComparingTo(new BigDecimal("10"));

        JsonNode creditNotes = json.readTree(mockMvc.perform(
                        get("/api/v1/accounting/customer-invoices/" + invoiceId + "/credit-notes")
                                .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(creditNotes).hasSize(1);
        assertThat(creditNotes.get(0).get("state").asText()).isEqualTo("DRAFT");
    }

    @Test
    void sales_order_return_endpoint_creates_draft_return() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("SO-OR-" + UUID.randomUUID().toString().substring(0, 6),
                "Order return button SO", categoryId, uomId, "10.00", "100.00");

        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "5", "10.00");
        validatePicking(receipt);

        UUID arAccountId = accountIdByCode("430003");
        UUID custId = createCustomer(arAccountId);

        String soBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":2,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode so = json.readTree(mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(soBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID soId = UUID.fromString(so.get("id").asText());
        confirmSalesOrder(soId);

        JsonNode ret = json.readTree(mockMvc.perform(post("/api/v1/sales/orders/" + soId + "/return")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(ret.get("pickingType").asText()).isEqualTo("INCOMING");
        assertThat(ret.get("state").asText()).isEqualTo("DRAFT");
        assertThat(ret.get("salesOrderId").asText()).isEqualTo(soId.toString());
    }

    @Test
    void customer_payment_respects_credit_notes_in_net_outstanding() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("SO-NET-" + UUID.randomUUID().toString().substring(0, 6),
                "Net outstanding test", categoryId, uomId, "10.00", "100.00");

        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "5", "10.00");
        validatePicking(receipt);

        UUID arAccountId = accountIdByCode("430003");
        UUID custId = createCustomer(arAccountId);
        UUID cashJournalId = journalIdByType("CASH");

        String soBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":2,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode so = json.readTree(mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(soBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID soId = UUID.fromString(so.get("id").asText());
        assertThat(so.get("state").asText()).isEqualTo("DRAFT");
        so = confirmSalesOrder(soId);
        assertThat(so.get("state").asText()).isEqualTo("CONFIRMED");
        UUID deliveryPickingId = UUID.fromString(so.get("deliveryPickingIds").get(0).asText());

        JsonNode invoices = json.readTree(mockMvc.perform(get("/api/v1/accounting/customer-invoices")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID invoiceId = null;
        for (JsonNode inv : invoices) {
            if (inv.has("salesOrderId") && soId.toString().equals(inv.get("salesOrderId").asText())
                    && "POSTED".equals(inv.get("state").asText())) {
                invoiceId = UUID.fromString(inv.get("id").asText());
                break;
            }
        }
        assertThat(invoiceId).isNotNull();

        JsonNode returnPicking = json.readTree(mockMvc.perform(post("/api/v1/inventory/pickings/" + deliveryPickingId + "/return")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID returnPickingId = UUID.fromString(returnPicking.get("id").asText());
        mockMvc.perform(post("/api/v1/inventory/pickings/" + returnPickingId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/inventory/pickings/" + returnPickingId + "/assign")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
        UUID returnMoveId = firstMoveId(returnPickingId);
        validatePickingWithPicks(returnPickingId, returnMoveId, "1", false);

        JsonNode creditNotes = json.readTree(mockMvc.perform(
                        get("/api/v1/accounting/customer-invoices/" + invoiceId + "/credit-notes")
                                .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(creditNotes).hasSize(1);
        UUID creditNoteId = UUID.fromString(creditNotes.get(0).get("id").asText());
        mockMvc.perform(post("/api/v1/accounting/customer-invoices/" + creditNoteId + "/post")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());

        String overpayBody = "{\"customerInvoiceId\":\"" + invoiceId + "\",\"paymentJournalId\":\"" + cashJournalId
                + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":200,\"currencyCode\":\"USD\",\"reference\":\"PAY-OVER\"}";
        mockMvc.perform(post("/api/v1/accounting/customer-invoices/payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overpayBody))
                .andExpect(status().isUnprocessableEntity());

        String netPayBody = "{\"customerInvoiceId\":\"" + invoiceId + "\",\"paymentJournalId\":\"" + cashJournalId
                + "\",\"paymentDate\":\"2026-05-04T12:00:00\",\"amount\":100,\"currencyCode\":\"USD\",\"reference\":\"PAY-NET\"}";
        mockMvc.perform(post("/api/v1/accounting/customer-invoices/payments")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(netPayBody))
                .andExpect(status().isOk());
    }

    @Test
    void sales_dashboard_returns_kpis_for_period() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("SO-DASH-" + UUID.randomUUID().toString().substring(0, 6),
                "Dashboard Product", categoryId, uomId, "10.00", "100.00");

        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "10", "10.00");
        validatePicking(receipt);

        UUID arAccountId = accountIdByCode("430003");
        UUID custId = createCustomer(arAccountId);

        String draftBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"orderDate\":\"2026-09-10\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Quote line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":1,\"unitPrice\":80,\"discountPercent\":0,\"taxIds\":[]}]}";
        mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody))
                .andExpect(status().isOk());

        String soBody = "{\"customerPartnerId\":\"" + custId + "\",\"currencyCode\":\"USD\",\"warehouseId\":\"" + warehouse
                + "\",\"orderDate\":\"2026-09-12\",\"lines\":[{\"productId\":\"" + productId + "\",\"name\":\"Order line\",\"uomId\":\"" + uomId
                + "\",\"qtyOrdered\":2,\"unitPrice\":100,\"discountPercent\":0,\"taxIds\":[]}]}";
        JsonNode so = json.readTree(mockMvc.perform(post("/api/v1/sales/orders")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(soBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(so.get("state").asText()).isEqualTo("DRAFT");
        so = confirmSalesOrder(UUID.fromString(so.get("id").asText()));
        assertThat(so.get("state").asText()).isEqualTo("CONFIRMED");

        JsonNode dash = json.readTree(mockMvc.perform(get("/api/v1/sales/dashboard")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // First order in this test remains draft (quotation count); second was confirmed.
        assertThat(dash.get("quotations").get("value").decimalValue()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(dash.get("orders").get("value").decimalValue()).isGreaterThanOrEqualTo(new BigDecimal("2"));
        assertThat(dash.get("revenue").get("value").decimalValue()).isGreaterThanOrEqualTo(new BigDecimal("280"));
        assertThat(dash.get("averageOrder").get("value").decimalValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(dash.get("series").isArray()).isTrue();
        assertThat(dash.get("topOrders").isArray()).isTrue();
        assertThat(dash.get("topOrders").size()).isGreaterThanOrEqualTo(1);
        assertThat(dash.get("topProducts").isArray()).isTrue();
        assertThat(dash.get("topProducts").size()).isGreaterThanOrEqualTo(1);
        assertThat(dash.get("topCategories").isArray()).isTrue();
        assertThat(dash.get("topCategories").size()).isGreaterThanOrEqualTo(1);
        assertThat(dash.get("channels").isArray()).isTrue();
        assertThat(dash.get("paymentMethods").isArray()).isTrue();
        assertThat(dash.get("granularity").asText()).isEqualTo("DAY");
    }

    private JsonNode confirmSalesOrder(UUID soId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sales/orders/" + soId + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode so = json.readTree(result.getResponse().getContentAsString());
        return deliverAndInvoiceSalesOrder(so);
    }

    /** Manual steps after confirm: validate open deliveries, then create and post the customer invoice. */
    private JsonNode deliverAndInvoiceSalesOrder(JsonNode so) throws Exception {
        UUID soId = UUID.fromString(so.get("id").asText());
        if (so.has("deliveryPickingIds") && so.get("deliveryPickingIds").isArray()) {
            for (JsonNode pickingIdNode : so.get("deliveryPickingIds")) {
                UUID pickingId = UUID.fromString(pickingIdNode.asText());
                JsonNode picking = json.readTree(mockMvc.perform(get("/api/v1/inventory/pickings/" + pickingId)
                                .header("X-Company-Id", COMPANY_ID.toString()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
                String state = picking.get("state").asText();
                if ("DONE".equals(state) || "CANCELLED".equals(state)) {
                    continue;
                }
                mockMvc.perform(post("/api/v1/sales/deliveries/" + pickingId + "/validate")
                                .header("X-Company-Id", COMPANY_ID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());
            }
        }
        MvcResult refreshed = mockMvc.perform(get("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        so = json.readTree(refreshed.getResponse().getContentAsString());
        if (so.has("canCreateCustomerInvoice") && so.get("canCreateCustomerInvoice").asBoolean()) {
            String invCmd = "{\"salesOrderId\":\"" + soId + "\",\"invoiceDate\":\"2026-05-04\",\"dueDate\":\"2026-06-04\"}";
            MvcResult invRes = mockMvc.perform(post("/api/v1/sales/customer-invoices/from-order")
                            .header("X-Company-Id", COMPANY_ID.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invCmd))
                    .andExpect(status().isOk())
                    .andReturn();
            UUID invoiceId = UUID.fromString(json.readTree(invRes.getResponse().getContentAsString()).get("id").asText());
            mockMvc.perform(post("/api/v1/accounting/customer-invoices/" + invoiceId + "/post")
                            .header("X-Company-Id", COMPANY_ID.toString()))
                    .andExpect(status().isOk());
        }
        refreshed = mockMvc.perform(get("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(refreshed.getResponse().getContentAsString());
    }

    private JsonNode validateOpenReturnPickings(JsonNode so) throws Exception {
        UUID soId = UUID.fromString(so.get("id").asText());
        if (so.has("returnPickingIds") && so.get("returnPickingIds").isArray()) {
            for (JsonNode pickingIdNode : so.get("returnPickingIds")) {
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
        MvcResult refreshed = mockMvc.perform(get("/api/v1/sales/orders/" + soId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(refreshed.getResponse().getContentAsString());
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
