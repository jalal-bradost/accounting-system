package com.bradox.erp.documents.service.domain.dto;

import java.io.InputStream;

/** File bytes for download. {@code inlineSafe} tells the controller whether the browser may render it. */
public record DocumentContent(InputStream stream, String fileName, String contentType, long sizeBytes, boolean inlineSafe) {
}
