package com.bradox.erp.documents.domain.core.rule;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTypePolicyTest {

    private static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            b[i] = (byte) values[i];
        }
        return b;
    }

    private static Optional<String> detect(byte[] head, String name) {
        return FileTypePolicy.detect(head, head.length, name);
    }

    @Test
    void acceptsPdfOnlyWhenExtensionMatches() {
        byte[] pdf = "%PDF-1.7 ...".getBytes(StandardCharsets.US_ASCII);
        assertEquals(Optional.of(FileTypePolicy.PDF), detect(pdf, "scan.PDF"));
        assertEquals(Optional.empty(), detect(pdf, "scan.png"));
    }

    @Test
    void acceptsImages() {
        assertEquals(Optional.of(FileTypePolicy.PNG),
                detect(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0), "a.png"));
        assertEquals(Optional.of(FileTypePolicy.JPEG), detect(bytes(0xFF, 0xD8, 0xFF, 0xE0), "a.jpg"));
        assertEquals(Optional.of(FileTypePolicy.JPEG), detect(bytes(0xFF, 0xD8, 0xFF, 0xE0), "a.jpeg"));
        assertEquals(Optional.of(FileTypePolicy.GIF), detect("GIF89a".getBytes(StandardCharsets.US_ASCII), "a.gif"));
        byte[] webp = bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P');
        assertEquals(Optional.of(FileTypePolicy.WEBP), detect(webp, "a.webp"));
        assertEquals(Optional.empty(), detect(webp, "a.png"));
    }

    @Test
    void zipFamilyIsDecidedByExtension() {
        byte[] zip = bytes('P', 'K', 3, 4, 0, 0);
        assertEquals(Optional.of(FileTypePolicy.DOCX), detect(zip, "a.docx"));
        assertEquals(Optional.of(FileTypePolicy.XLSX), detect(zip, "a.xlsx"));
        assertEquals(Optional.of(FileTypePolicy.PPTX), detect(zip, "a.pptx"));
        assertEquals(Optional.of(FileTypePolicy.ZIP), detect(zip, "a.zip"));
        assertEquals(Optional.empty(), detect(zip, "a.exe"));
    }

    @Test
    void oleFamilyIsDecidedByExtension() {
        byte[] ole = bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1, 0);
        assertEquals(Optional.of(FileTypePolicy.DOC), detect(ole, "a.doc"));
        assertEquals(Optional.of(FileTypePolicy.XLS), detect(ole, "a.xls"));
        assertEquals(Optional.of(FileTypePolicy.PPT), detect(ole, "a.ppt"));
        assertEquals(Optional.empty(), detect(ole, "a.pdf"));
    }

    @Test
    void acceptsPlainTextFamilyIncludingUtf8Arabic() {
        assertEquals(Optional.of(FileTypePolicy.TXT), detect("hello".getBytes(StandardCharsets.UTF_8), "a.txt"));
        assertEquals(Optional.of(FileTypePolicy.CSV), detect("a,b\n1,2".getBytes(StandardCharsets.UTF_8), "a.csv"));
        assertEquals(Optional.of(FileTypePolicy.JSON), detect("{\"a\":1}".getBytes(StandardCharsets.UTF_8), "a.json"));
        assertEquals(Optional.of(FileTypePolicy.XML),
                detect("<?xml version=\"1.0\"?><a/>".getBytes(StandardCharsets.UTF_8), "ready_mat.xml"));
        assertEquals(Optional.of(FileTypePolicy.TXT),
                detect("مرحبا".getBytes(StandardCharsets.UTF_8), "a.txt"));
    }

    @Test
    void rejectsBinaryDisguisedAsTextAndMarkup() {
        assertEquals(Optional.empty(), detect(bytes('M', 'Z', 0, 0), "a.txt"));
        assertEquals(Optional.empty(), detect(bytes(0xC3, 0x28), "a.txt"));
        assertEquals(Optional.empty(), detect("<html><script>x</script></html>".getBytes(StandardCharsets.UTF_8), "a.txt"));
        assertEquals(Optional.empty(), detect("<svg xmlns=\"x\"/>".getBytes(StandardCharsets.UTF_8), "a.xml"));
    }

    @Test
    void rejectsEmptyUnknownAndHtml() {
        assertEquals(Optional.empty(), FileTypePolicy.detect(null, 0, "a.pdf"));
        assertEquals(Optional.empty(), FileTypePolicy.detect(new byte[0], 0, "a.pdf"));
        assertEquals(Optional.empty(), detect("<html></html>".getBytes(StandardCharsets.UTF_8), "a.html"));
        assertEquals(Optional.empty(), detect("x".getBytes(StandardCharsets.UTF_8), "noext"));
    }

    @Test
    void toleratesMultiByteSequenceCutAtSniffWindow() {
        byte[] head = new byte[FileTypePolicy.SNIFF_BYTES];
        java.util.Arrays.fill(head, (byte) 'a');
        // a 2-byte character split at the end of the window
        head[head.length - 1] = (byte) 0xD8;
        assertEquals(Optional.of(FileTypePolicy.TXT), FileTypePolicy.detect(head, head.length, "a.txt"));
    }

    @Test
    void inlineSafeAndImageHelpers() {
        assertTrue(FileTypePolicy.isInlineSafe("application/pdf"));
        assertTrue(FileTypePolicy.isInlineSafe("IMAGE/PNG"));
        assertFalse(FileTypePolicy.isInlineSafe("text/plain"));
        assertFalse(FileTypePolicy.isInlineSafe(null));
        assertTrue(FileTypePolicy.isImage("image/webp"));
        assertFalse(FileTypePolicy.isImage("application/pdf"));
        assertFalse(FileTypePolicy.isImage(null));
    }

    @Test
    void extensionOf() {
        assertEquals("pdf", FileTypePolicy.extensionOf("a.b.PDF"));
        assertEquals("", FileTypePolicy.extensionOf("noext"));
        assertEquals("", FileTypePolicy.extensionOf("trailing."));
        assertEquals("", FileTypePolicy.extensionOf(null));
    }
}
