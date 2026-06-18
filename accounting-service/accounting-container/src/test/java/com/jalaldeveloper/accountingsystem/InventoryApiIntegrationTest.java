package com.jalaldeveloper.accountingsystem;

import com.jalaldeveloper.accountingsystem.platform.bootstrap.PlatformRbacSeeder;
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
import java.math.RoundingMode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end inventory flows over REST. Exercises the full pipeline (REST -> service ->
 * domain -> JPA -> H2) for the canonical receipt / delivery / backorder / adjustment scenarios.
 *
 * <p>Relies on the bootstrap seeders (warehouse {@code WH}, virtual SUPPLIERS / CUSTOMERS /
 * INVENTORY-LOSS locations, product category "All", chart of accounts) for the demo company.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void receive_andDeliver_keepsValuationConsistent() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID customer = lookupLocationByCode("VIRT/CUSTOMERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");

        UUID productId = createProduct("WIDGET-" + UUID.randomUUID().toString().substring(0, 8),
                "Widget", categoryId, uomId, "10.00", "25.00");

        // 1) Receipt: 5 widgets @ 10.00 each
        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "5", "10.00");
        validatePicking(receipt);

        assertOnHand(productId, "5");
        assertValuation(productId, "50.00");

        // 2) Receipt: 5 more widgets @ 14.00 each (AVCO running average -> 12.00 / unit, total 120.00)
        UUID receipt2 = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "5", "14.00");
        validatePicking(receipt2);

        assertOnHand(productId, "10");
        assertValuation(productId, "120.00");

        // 3) Delivery: 4 widgets - AVCO @ 12.00 -> 48.00 leaves stock
        UUID delivery = createPicking(warehouse, "OUTGOING", stockLoc, customer, productId, uomId, "4", null);
        confirmPicking(delivery);
        assignPicking(delivery);
        validatePicking(delivery);

        assertOnHand(productId, "6");
        assertValuationApprox(productId, new BigDecimal("72.00"), new BigDecimal("0.50"));

        // 4) Inventory adjustment: bring on-hand down to 5 (write-off 1 unit)
        adjustInventory(productId, stockLoc, "5", "Cycle count");
        assertOnHand(productId, "5");
    }

    @Test
    void deliver_partial_createsBackorderInDraft() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID supplier = lookupLocationByCode("VIRT/SUPPLIERS");
        UUID customer = lookupLocationByCode("VIRT/CUSTOMERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");

        UUID productId = createProduct("BACKORDER-" + UUID.randomUUID().toString().substring(0, 8),
                "Backorder Widget", categoryId, uomId, "5.00", "15.00");

        // Receive 3
        UUID receipt = createPicking(warehouse, "INCOMING", supplier, stockLoc, productId, uomId, "3", "5.00");
        validatePicking(receipt);
        assertOnHand(productId, "3");

        // Try to deliver 10; only 3 reservable. We validate WITHOUT picked override: the
        // assign step reserves 3 of 10; then validatePicking with no overrides validates the
        // full demand=10 (which would exceed on-hand). Use a per-move override with 3.
        UUID delivery = createPicking(warehouse, "OUTGOING", stockLoc, customer, productId, uomId, "10", null);
        confirmPicking(delivery);
        assignPicking(delivery);

        // Fetch the move id of the only move in this picking.
        UUID moveId = firstMoveId(delivery);
        validatePickingWithPicks(delivery, moveId, "3", true);

        // After validating with picked=3 and demand=10, a backorder picking should exist for 7 units in DRAFT state.
        MvcResult listResult = mockMvc.perform(get("/api/v1/inventory/pickings")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .param("pickingType", "OUTGOING")
                        .param("state", "DRAFT")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pickings = json.readTree(listResult.getResponse().getContentAsString()).get("content");
        boolean hasBackorder = false;
        for (JsonNode p : pickings) {
            if (p.has("backorderOf") && delivery.toString().equals(p.get("backorderOf").asText())) {
                hasBackorder = true;
                assertThat(p.get("moves").get(0).get("demandQuantity").decimalValue())
                        .isEqualByComparingTo(new BigDecimal("7"));
            }
        }
        assertThat(hasBackorder)
                .as("expected a draft backorder picking for the unfulfilled 7 units")
                .isTrue();
        assertOnHand(productId, "0");
    }

    @Test
    void deliver_withoutStock_failsWithDomainError() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID customer = lookupLocationByCode("VIRT/CUSTOMERS");
        UUID warehouse = lookupWarehouseByCode("WH");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");

        UUID productId = createProduct("EMPTY-" + UUID.randomUUID().toString().substring(0, 8),
                "Empty Widget", categoryId, uomId, "1.00", "2.00");

        // Outgoing on a product with no stock -> validation should fail because applyDelta on
        // the source quant would go negative and the location forbids it.
        UUID delivery = createPicking(warehouse, "OUTGOING", stockLoc, customer, productId, uomId, "2", null);
        confirmPicking(delivery);

        mockMvc.perform(post("/api/v1/inventory/pickings/" + delivery + "/validate")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"createBackorder\":false}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVENTORY_DOMAIN_ERROR"));
    }

    @Test
    void inventoryAdjustment_noOp_returnsDomainError() throws Exception {
        UUID stockLoc = lookupLocationByCode("WH/STOCK");
        UUID categoryId = lookupCategoryByName("All");
        UUID uomId = lookupUomByName("Unit");

        UUID productId = createProduct("NOOP-" + UUID.randomUUID().toString().substring(0, 8),
                "Noop Widget", categoryId, uomId, "1.00", "1.00");

        // Adjust to 0 when current is already 0.
        String body = "{\"productId\":\"" + productId + "\",\"locationId\":\"" + stockLoc
                + "\",\"targetQuantity\":0,\"reason\":\"noop\"}";
        mockMvc.perform(post("/api/v1/inventory/pickings/adjust")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVENTORY_DOMAIN_ERROR"));
    }

    @Test
    void uomConversion_returnsConvertedQuantity() throws Exception {
        UUID dozen = lookupUomByName("Dozen");
        UUID unit = lookupUomByName("Unit");
        mockMvc.perform(get("/api/v1/inventory/uoms/convert")
                        .header("X-Company-Id", COMPANY_ID.toString())
                        .param("fromUomId", dozen.toString())
                        .param("toUomId", unit.toString())
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convertedQuantity").value(24));
    }

    // ============= Helpers =============

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
        throw new AssertionError("seeded location not found: " + code);
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
        throw new AssertionError("seeded warehouse not found: " + code);
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
        throw new AssertionError("seeded product category not found: " + name);
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
        throw new AssertionError("seeded uom not found: " + name);
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

    private void confirmPicking(UUID id) throws Exception {
        mockMvc.perform(post("/api/v1/inventory/pickings/" + id + "/confirm")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
    }

    private void assignPicking(UUID id) throws Exception {
        mockMvc.perform(post("/api/v1/inventory/pickings/" + id + "/assign")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk());
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

    private void adjustInventory(UUID productId, UUID locationId, String targetQty, String reason) throws Exception {
        String body = "{\"productId\":\"" + productId + "\",\"locationId\":\"" + locationId
                + "\",\"targetQuantity\":" + targetQty + ",\"reason\":\"" + reason + "\"}";
        mockMvc.perform(post("/api/v1/inventory/pickings/adjust")
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

    private void assertOnHand(UUID productId, String expected) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/inventory/on-hand/" + productId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        BigDecimal actual = json.readTree(result.getResponse().getContentAsString())
                .get("totalOnHand").decimalValue();
        assertThat(actual).isEqualByComparingTo(new BigDecimal(expected));
    }

    private void assertValuation(UUID productId, String expected) throws Exception {
        assertValuationApprox(productId, new BigDecimal(expected), new BigDecimal("0.005"));
    }

    private void assertValuationApprox(UUID productId, BigDecimal expected, BigDecimal tolerance) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/inventory/valuation/" + productId)
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andReturn();
        BigDecimal actual = json.readTree(result.getResponse().getContentAsString())
                .get("valuation").decimalValue().setScale(4, RoundingMode.HALF_UP);
        assertThat(actual.subtract(expected).abs())
                .as("valuation %s within tolerance %s of expected %s", actual, tolerance, expected)
                .isLessThanOrEqualTo(tolerance);
    }
}
