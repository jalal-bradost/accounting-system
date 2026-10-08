package com.bradox.erp.documents.domain.core.rule;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which files are accepted by looking at the content, not only at the declared type.
 * Executables, scripts, HTML and SVG are never accepted. The detected type is what gets stored
 * and served, so a renamed file cannot pass as a PDF.
 */
public final class FileTypePolicy {

    public static final String PDF = "application/pdf";
    public static final String PNG = "image/png";
    public static final String JPEG = "image/jpeg";
    public static final String GIF = "image/gif";
    public static final String WEBP = "image/webp";
    public static final String ZIP = "application/zip";
    public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    public static final String DOC = "application/msword";
    public static final String XLS = "application/vnd.ms-excel";
    public static final String PPT = "application/vnd.ms-powerpoint";
    public static final String TXT = "text/plain";
    public static final String CSV = "text/csv";
    public static final String XML = "application/xml";
    public static final String JSON = "application/json";

    /** Number of leading bytes the caller should pass to {@link #detect}. */
    public static final int SNIFF_BYTES = 8192;

    private static final Map<String, String> ZIP_FAMILY = Map.of(
            "docx", DOCX, "xlsx", XLSX, "pptx", PPTX, "zip", ZIP);
    private static final Map<String, String> OLE_FAMILY = Map.of(
            "doc", DOC, "xls", XLS, "ppt", PPT);
    private static final Map<String, String> TEXT_FAMILY = Map.of(
            "txt", TXT, "csv", CSV, "xml", XML, "json", JSON, "md", TXT, "log", TXT);
    private static final Set<String> INLINE_SAFE = Set.of(PDF, PNG, JPEG, GIF, WEBP);

    private FileTypePolicy() {
    }

    /** Returns the verified content type, or empty when the file must be rejected. */
    public static Optional<String> detect(byte[] head, int length, String fileName) {
        if (head == null || length <= 0) {
            return Optional.empty();
        }
        String ext = extensionOf(fileName);
        if (startsWith(head, length, '%', 'P', 'D', 'F', '-')) {
            return "pdf".equals(ext) ? Optional.of(PDF) : Optional.empty();
        }
        if (startsWith(head, length, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "png".equals(ext) ? Optional.of(PNG) : Optional.empty();
        }
        if (startsWith(head, length, 0xFF, 0xD8, 0xFF)) {
            return ("jpg".equals(ext) || "jpeg".equals(ext)) ? Optional.of(JPEG) : Optional.empty();
        }
        if (startsWith(head, length, 'G', 'I', 'F', '8')) {
            return "gif".equals(ext) ? Optional.of(GIF) : Optional.empty();
        }
        if (length >= 12 && startsWith(head, length, 'R', 'I', 'F', 'F')
                && (head[8] & 0xFF) == 'W' && (head[9] & 0xFF) == 'E'
                && (head[10] & 0xFF) == 'B' && (head[11] & 0xFF) == 'P') {
            return "webp".equals(ext) ? Optional.of(WEBP) : Optional.empty();
        }
        if (startsWith(head, length, 'P', 'K', 0x03, 0x04)) {
            return Optional.ofNullable(ZIP_FAMILY.get(ext));
        }
        if (startsWith(head, length, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)) {
            return Optional.ofNullable(OLE_FAMILY.get(ext));
        }
        String textType = TEXT_FAMILY.get(ext);
        if (textType != null && looksLikeText(head, length) && !looksLikeMarkup(head, length, ext)) {
            return Optional.of(textType);
        }
        return Optional.empty();
    }

    /** True when the browser may render the file inline. Everything else is forced to download. */
    public static boolean isInlineSafe(String contentType) {
        return contentType != null && INLINE_SAFE.contains(contentType.toLowerCase(Locale.ROOT));
    }

    public static boolean isImage(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
    }

    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean startsWith(byte[] head, int length, int... expected) {
        if (length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((head[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeText(byte[] head, int length) {
        for (int i = 0; i < length; i++) {
            if (head[i] == 0) {
                return false;
            }
        }
        // the sniff window may cut a multi-byte UTF-8 sequence, so tolerate up to 3 dangling bytes
        for (int cut = 0; cut <= 3 && length - cut > 0; cut++) {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            try {
                decoder.decode(ByteBuffer.wrap(head, 0, length - cut));
                return true;
            } catch (CharacterCodingException e) {
                if (length < SNIFF_BYTES) {
                    return false;
                }
            }
        }
        return false;
    }

    private static boolean looksLikeMarkup(byte[] head, int length, String ext) {
        String prefix = new String(head, 0, Math.min(length, 1024), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        if (prefix.contains("<script") || prefix.contains("<html") || prefix.contains("<!doctype html")) {
            return true;
        }
        return "xml".equals(ext) && prefix.contains("<svg");
    }
}
