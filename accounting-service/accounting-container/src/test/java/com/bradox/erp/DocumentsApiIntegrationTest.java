package com.bradox.erp;

import com.bradox.erp.platform.bootstrap.PlatformRbacSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Documents end to end over REST: folders, upload, search, download, versions, tags, links, trash. */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentsApiIntegrationTest {

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;
    private static final String BASE = "/api/v1/documents";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    private <T extends AbstractMockHttpServletRequestBuilder<T>> T withCompany(T b) {
        return b.header("X-Company-Id", COMPANY_ID.toString());
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private UUID createFolder(UUID parent, String name) throws Exception {
        String payload = "{\"name\":\"" + name + "\"" + (parent == null ? "" : ",\"parentId\":\"" + parent + "\"") + "}";
        MvcResult r = mockMvc.perform(withCompany(post(BASE + "/folders")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andReturn();
        return UUID.fromString(body(r).get("id").asText());
    }

    private JsonNode upload(UUID folder, String fileName, byte[] bytes, boolean allowDuplicate, int expectedStatus) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", fileName, "application/octet-stream", bytes);
        MvcResult r = mockMvc.perform(withCompany(multipart(BASE).file(file))
                        .param("folderId", folder.toString())
                        .param("allowDuplicate", String.valueOf(allowDuplicate)))
                .andExpect(status().is(expectedStatus)).andReturn();
        return body(r);
    }

    private static byte[] pdf(String marker) {
        return ("%PDF-1.4\n" + marker + "\n%%EOF").getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void defaultWorkspacesAreSeededAndFoldersFollowTheTreeRules() throws Exception {
        MvcResult list = mockMvc.perform(withCompany(get(BASE + "/folders"))).andExpect(status().isOk()).andReturn();
        JsonNode folders = body(list);
        assertThat(folders.size()).isGreaterThanOrEqualTo(6);
        boolean hasFinance = false;
        for (JsonNode f : folders) {
            if ("FINANCE".equals(f.path("systemKey").asText())) {
                hasFinance = true;
            }
        }
        assertThat(hasFinance).isTrue();

        String unique = "Reports-" + UUID.randomUUID().toString().substring(0, 6);
        UUID parent = createFolder(null, unique);
        UUID child = createFolder(parent, "Q1");

        // same name under the same parent, even with different case, is rejected
        mockMvc.perform(withCompany(post(BASE + "/folders")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"q1\",\"parentId\":\"" + parent + "\"}"))
                .andExpect(status().isUnprocessableEntity());

        // a folder cannot move into its own child
        mockMvc.perform(withCompany(put(BASE + "/folders/" + parent)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"" + child + "\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(withCompany(put(BASE + "/folders/" + child)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Q2\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Q2"));

        mockMvc.perform(withCompany(post(BASE + "/folders/" + child + "/archive")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void uploadSearchDownloadVersionsAndTrash() throws Exception {
        UUID folder = createFolder(null, "Inbox-" + UUID.randomUUID().toString().substring(0, 6));
        byte[] v1 = pdf("version one " + UUID.randomUUID());

        JsonNode created = upload(folder, "Vendor Invoice 2026.pdf", v1, false, 200);
        assertThat(created.get("status").asText()).isEqualTo("CREATED");
        UUID docId = UUID.fromString(created.get("document").get("id").asText());
        assertThat(created.get("document").get("contentType").asText()).isEqualTo("application/pdf");
        assertThat(created.get("document").get("versionNo").asInt()).isEqualTo(1);

        // the same bytes in the same folder is a duplicate unless allowed
        JsonNode dup = upload(folder, "copy.pdf", v1, false, 200);
        assertThat(dup.get("status").asText()).isEqualTo("DUPLICATE");
        assertThat(dup.get("existing").get("id").asText()).isEqualTo(docId.toString());

        // a renamed executable is refused
        mockMvc.perform(withCompany(multipart(BASE).file(new MockMultipartFile("file", "invoice.pdf", "application/pdf",
                        new byte[]{'M', 'Z', 0, 0, 1, 2, 3})))
                        .param("folderId", folder.toString()))
                .andExpect(status().isUnprocessableEntity());

        // search by name, with Arabic-Indic digits normalized
        mockMvc.perform(withCompany(get(BASE)).param("q", "vendor invoice ٢٠٢٦").param("folderId", folder.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(docId.toString()));

        // download keeps the bytes and refuses sniffing
        MvcResult content = mockMvc.perform(withCompany(get(BASE + "/" + docId + "/content")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn();
        assertThat(content.getResponse().getContentAsByteArray()).isEqualTo(v1);

        // new version, then restore version 1 as version 3
        MockMultipartFile second = new MockMultipartFile("file", "Vendor Invoice 2026 v2.pdf", "application/pdf", pdf("version two"));
        mockMvc.perform(withCompany(multipart(BASE + "/" + docId + "/versions").file(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.document.versionNo").value(2));
        MvcResult versions = mockMvc.perform(withCompany(get(BASE + "/" + docId + "/versions"))).andExpect(status().isOk()).andReturn();
        assertThat(body(versions).size()).isEqualTo(2);
        UUID firstVersion = null;
        for (JsonNode v : body(versions)) {
            if (v.get("versionNo").asInt() == 1) {
                firstVersion = UUID.fromString(v.get("id").asText());
            }
        }
        mockMvc.perform(withCompany(post(BASE + "/" + docId + "/versions/" + firstVersion + "/restore")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versionNo").value(3));
        MvcResult restoredContent = mockMvc.perform(withCompany(get(BASE + "/" + docId + "/content"))).andExpect(status().isOk()).andReturn();
        assertThat(restoredContent.getResponse().getContentAsByteArray()).isEqualTo(v1);

        // rename and move
        UUID other = createFolder(null, "Archive-" + UUID.randomUUID().toString().substring(0, 6));
        mockMvc.perform(withCompany(patch(BASE + "/" + docId)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Paid invoice\",\"folderId\":\"" + other + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paid invoice"))
                .andExpect(jsonPath("$.folderId").value(other.toString()));

        // trash hides it, restore brings it back
        mockMvc.perform(withCompany(delete(BASE + "/" + docId))).andExpect(status().isNoContent());
        mockMvc.perform(withCompany(get(BASE)).param("folderId", other.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(withCompany(get(BASE)).param("status", "TRASHED").param("folderId", other.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(withCompany(post(BASE + "/" + docId + "/restore")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void retentionLockBlocksTrash() throws Exception {
        UUID folder = createFolder(null, "Signed-" + UUID.randomUUID().toString().substring(0, 6));
        JsonNode created = upload(folder, "contract.pdf", pdf("contract " + UUID.randomUUID()), false, 200);
        UUID docId = UUID.fromString(created.get("document").get("id").asText());
        mockMvc.perform(withCompany(post(BASE + "/" + docId + "/retention")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"until\":\"2099-01-01\",\"reason\":\"signed contract\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.retentionUntil").value("2099-01-01"));
        mockMvc.perform(withCompany(delete(BASE + "/" + docId))).andExpect(status().isUnprocessableEntity());
        mockMvc.perform(withCompany(delete(BASE + "/trash"))).andExpect(status().isOk());
        mockMvc.perform(withCompany(get(BASE + "/" + docId))).andExpect(status().isOk());
    }

    @Test
    void tagsFilterWithOrInsideAFacetAndAndAcrossFacets() throws Exception {
        UUID folder = createFolder(null, "Tagged-" + UUID.randomUUID().toString().substring(0, 6));
        UUID facetStatus = createFacet("Status");
        UUID facetType = createFacet("Type");
        UUID draft = createTag(facetStatus, "Draft");
        UUID signed = createTag(facetStatus, "Signed");
        UUID contract = createTag(facetType, "Contract");

        UUID a = uploadOne(folder, "a.pdf");
        UUID b = uploadOne(folder, "b.pdf");
        setTags(a, draft, contract);
        setTags(b, signed);

        // OR inside one facet: Draft or Signed finds both
        searchCount(folder, "tagIds=" + draft + "," + signed, 2);
        // AND across facets: (Draft or Signed) and Contract finds only a
        searchCount(folder, "tagIds=" + draft + "," + signed + "," + contract, 1);
        // text search also matches tag names
        MvcResult byTagName = mockMvc.perform(withCompany(get(BASE)).param("q", "signed").param("folderId", folder.toString()))
                .andExpect(status().isOk()).andReturn();
        assertThat(body(byTagName).get("totalElements").asInt()).isEqualTo(1);

        // deleting a tag removes it from documents
        mockMvc.perform(withCompany(delete(BASE + "/tags/" + draft))).andExpect(status().isNoContent());
        // a keeps only Contract, b keeps only Signed, so Signed AND Contract matches nothing
        searchCount(folder, "tagIds=" + signed + "," + contract, 0);
        searchCount(folder, "tagIds=" + contract, 1);
        MvcResult detail = mockMvc.perform(withCompany(get(BASE + "/" + a))).andExpect(status().isOk()).andReturn();
        assertThat(body(detail).get("tagIds").size()).isEqualTo(1);
    }

    @Test
    void linksToRecordsAreValidated() throws Exception {
        UUID folder = createFolder(null, "Linked-" + UUID.randomUUID().toString().substring(0, 6));
        UUID docId = uploadOne(folder, "agreement.pdf");

        mockMvc.perform(withCompany(post(BASE + "/" + docId + "/links")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelName\":\"unknown.model\",\"recordId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(withCompany(post(BASE + "/" + docId + "/links")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelName\":\"contacts.partner\",\"recordId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnprocessableEntity());

        MvcResult partner = mockMvc.perform(withCompany(post("/api/v1/contacts/partners")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"COMPANY\",\"displayName\":\"Linked Co " + UUID.randomUUID().toString().substring(0, 5)
                                + "\",\"customer\":false,\"vendor\":false}"))
                .andExpect(status().isOk()).andReturn();
        UUID partnerId = UUID.fromString(body(partner).get("id").asText());

        mockMvc.perform(withCompany(post(BASE + "/" + docId + "/links")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelName\":\"contacts.partner\",\"recordId\":\"" + partnerId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0].recordId").value(partnerId.toString()))
                .andExpect(jsonPath("$.links[0].label").isNotEmpty());
        // adding the same link twice keeps one
        mockMvc.perform(withCompany(post(BASE + "/" + docId + "/links")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelName\":\"contacts.partner\",\"recordId\":\"" + partnerId + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.links.length()").value(1));

        mockMvc.perform(withCompany(get(BASE + "/by-record")).param("model", "contacts.partner").param("recordId", partnerId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(docId.toString()));
        mockMvc.perform(withCompany(get(BASE)).param("linkedModel", "contacts.partner").param("folderId", folder.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(withCompany(delete(BASE + "/" + docId + "/links")).param("model", "contacts.partner")
                        .param("recordId", partnerId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.links.length()").value(0));
    }

    // ------------------------------------------------------------ helpers

    private UUID uploadOne(UUID folder, String name) throws Exception {
        JsonNode created = upload(folder, name, pdf(name + UUID.randomUUID()), false, 200);
        return UUID.fromString(created.get("document").get("id").asText());
    }

    private UUID createFacet(String name) throws Exception {
        MvcResult r = mockMvc.perform(withCompany(post(BASE + "/tag-facets")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return UUID.fromString(body(r).get("id").asText());
    }

    private UUID createTag(UUID facet, String name) throws Exception {
        MvcResult r = mockMvc.perform(withCompany(post(BASE + "/tags")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facetId\":\"" + facet + "\",\"name\":\"" + name + "\",\"color\":\"#16a34a\"}"))
                .andExpect(status().isOk()).andReturn();
        return UUID.fromString(body(r).get("id").asText());
    }

    private void setTags(UUID docId, UUID... tags) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (UUID t : tags) {
            ids.append(ids.length() == 0 ? "" : ",").append('"').append(t).append('"');
        }
        mockMvc.perform(withCompany(put(BASE + "/" + docId + "/tags")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[" + ids + "]}"))
                .andExpect(status().isOk());
    }

    private void searchCount(UUID folder, String tagQuery, int expected) throws Exception {
        String ids = tagQuery.substring(tagQuery.indexOf('=') + 1);
        MvcResult r = mockMvc.perform(withCompany(get(BASE)).param("folderId", folder.toString()).param("tagIds", ids))
                .andExpect(status().isOk()).andReturn();
        assertThat(body(r).get("totalElements").asInt()).isEqualTo(expected);
    }
}
