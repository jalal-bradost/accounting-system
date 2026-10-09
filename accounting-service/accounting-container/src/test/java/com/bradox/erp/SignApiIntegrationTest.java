package com.bradox.erp;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.JsonNode;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sign end to end: upload a PDF as a template, place boxes, sign it on screen, download the signed copy. */
@SpringBootTest
@AutoConfigureMockMvc
class SignApiIntegrationTest extends SalesScenarioSupport {

    private static final String BASE = "/api/v1/sign";

    private static byte[] pdf(int pages) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static String signaturePng() throws Exception {
        BufferedImage img = new BufferedImage(200, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.drawLine(5, 50, 195, 10);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private JsonNode createTemplate() throws Exception {
        var r = mockMvc.perform(multipart(BASE + "/templates")
                        .file(new MockMultipartFile("file", "Contract.pdf", "application/pdf", pdf(2)))
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private static String field(int page, double y, String type) {
        return "{\"page\":" + page + ",\"x\":0.1,\"y\":" + y + ",\"width\":0.3,\"height\":0.05,\"type\":\"" + type + "\"}";
    }

    @Test
    void signsATemplateOnScreen() throws Exception {
        JsonNode t = createTemplate();
        String id = t.get("id").asText();
        assertThat(t.get("name").asText()).isEqualTo("Contract");
        assertThat(t.get("pageCount").asInt()).isEqualTo(2);

        // No signature box yet: signing is refused.
        call(post(BASE + "/templates/" + id + "/sign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"signerName\":\"Ali\",\"values\":[]}")).andExpect(status().is4xxClientError());

        JsonNode saved = call(put(BASE + "/templates/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sales contract\",\"fields\":[" + field(2, 0.8, "SIGNATURE") + "," + field(2, 0.7, "NAME") + ","
                        + field(2, 0.6, "DATE") + "," + field(1, 0.1, "TEXT") + "]}"))
                .andExpect(status().isOk()).json();
        assertThat(saved.get("fields").size()).isEqualTo(4);
        String signatureField = saved.get("fields").get(0).get("id").asText();
        String textField = saved.get("fields").get(3).get("id").asText();

        call(get(BASE + "/templates/" + id + "/pages/1/image")).andExpect(status().isOk());

        // A missing signature is refused.
        call(post(BASE + "/templates/" + id + "/sign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"signerName\":\"Ali\",\"values\":[]}")).andExpect(status().is4xxClientError());

        JsonNode signed = call(post(BASE + "/templates/" + id + "/sign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"signerName\":\"Ali Hasan\",\"values\":[{\"fieldId\":\"" + signatureField + "\",\"imagePng\":\""
                        + signaturePng() + "\"},{\"fieldId\":\"" + textField + "\",\"text\":\"Erbil\"}]}"))
                .andExpect(status().isCreated()).json();
        assertThat(signed.get("signerName").asText()).isEqualTo("Ali Hasan");
        assertThat(signed.get("templateName").asText()).isEqualTo("Sales contract");

        JsonNode list = call(get(BASE + "/templates/" + id + "/signed")).andExpect(status().isOk()).json();
        assertThat(list.size()).isEqualTo(1);
        assertThat(call(get(BASE + "/templates/" + id)).json().get("signedCount").asLong()).isEqualTo(1);

        byte[] file = mockMvc.perform(get(BASE + "/signed/" + signed.get("id").asText() + "/download")
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (PDDocument doc = PDDocument.load(file)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
        }

        call(delete(BASE + "/templates/" + id)).andExpect(status().isNoContent());
        call(get(BASE + "/templates/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void refusesFilesThatAreNotPdf() throws Exception {
        mockMvc.perform(multipart(BASE + "/templates")
                        .file(new MockMultipartFile("file", "notes.pdf", "application/pdf", "hello".getBytes()))
                        .header("X-Company-Id", COMPANY_ID.toString()))
                .andExpect(status().is4xxClientError());
    }
}
