package com.jalaldeveloper.accountingsystem;

import com.jalaldeveloper.accountingsystem.platform.bootstrap.PlatformRbacSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PosApiIntegrationTest {
    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void checkout_sellsStockedProductAndCreatesReceipt() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");
        UUID productId = createProduct("POS-FLOW-" + UUID.randomUUID().toString().substring(0, 6),
                "POS Flow", categoryId, uomId, "10.00", "25.00");
        UUID receiptPicking = createPicking(warehouse, supplier, stockLoc, productId, uomId, "4", "10.00");
        validatePicking(receiptPicking);

        UUID arAccountId = accountIdByCode("430003");
        UUID customerId = createCustomer(arAccountId);
        UUID cashJournalId = journalIdByType("CASH");

        String configBody = "{\"name\":\"POS Test " + UUID.randomUUID().toString().substring(0, 6)
                + "\",\"warehouseId\":\"" + warehouse
                + "\",\"defaultCustomerPartnerId\":\"" + customerId
                + "\",\"cashJournalId\":\"" + cashJournalId
                + "\",\"currencyCode\":\"USD\"}";
        MvcResult configResult = mockMvc.perform(post("/api/v1/pos/configs")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody))
                .andExpect(status().isOk())
                .andReturn();
        UUID configId = UUID.fromString(json.readTree(configResult.getResponse().getContentAsString()).get("id").asText());

        MvcResult sessionResult = mockMvc.perform(post("/api/v1/pos/sessions")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configId\":\"" + configId + "\",\"openingCash\":100}"))
                .andExpect(status().isOk())
                .andReturn();
        UUID sessionId = UUID.fromString(json.readTree(sessionResult.getResponse().getContentAsString()).get("id").asText());

        String checkoutBody = "{\"sessionId\":\"" + sessionId
                + "\",\"lines\":[{\"productId\":\"" + productId
                + "\",\"name\":\"POS Flow\",\"uomId\":\"" + uomId
                + "\",\"quantity\":2,\"unitPrice\":25,\"discountPercent\":0,\"taxIds\":[]}]"
                + ",\"payments\":[{\"method\":\"CASH\",\"amount\":50,\"reference\":\"POS-CASH\"}]}";
        MvcResult checkoutResult = mockMvc.perform(post("/api/v1/pos/checkout")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode order = json.readTree(checkoutResult.getResponse().getContentAsString());
        assertThat(order.get("state").asText()).isEqualTo("FINALIZED");
        assertThat(order.get("salesOrderId").asText()).isNotBlank();
        assertThat(order.get("customerInvoiceId").asText()).isNotBlank();
        assertThat(order.get("receiptId").asText()).isNotBlank();
        assertThat(order.get("amountTotal").decimalValue()).isEqualByComparingTo("50.0000");

        mockMvc.perform(get("/api/v1/pos/receipts/" + order.get("receiptId").asText())
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
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

    private UUID createPicking(UUID warehouseId, UUID sourceLoc, UUID destLoc,
                               UUID productId, UUID uomId, String qty, String unitCost) throws Exception {
        String body = "{\"companyId\":\"" + COMPANY_ID + "\",\"warehouseId\":\"" + warehouseId
                + "\",\"pickingType\":\"INCOMING\",\"sourceLocationId\":\"" + sourceLoc
                + "\",\"destinationLocationId\":\"" + destLoc
                + "\",\"moves\":[{\"productId\":\"" + productId
                + "\",\"uomId\":\"" + uomId
                + "\",\"demandQuantity\":" + qty
                + ",\"unitCost\":" + unitCost + "}]}";
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

    private UUID createCustomer(UUID receivableAccountId) throws Exception {
        String body = "{\"kind\":\"COMPANY\",\"displayName\":\"POS Cust " + UUID.randomUUID().toString().substring(0, 6)
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
