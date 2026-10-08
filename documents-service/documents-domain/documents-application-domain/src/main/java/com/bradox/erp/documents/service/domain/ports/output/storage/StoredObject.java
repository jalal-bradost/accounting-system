package com.bradox.erp.documents.service.domain.ports.output.storage;

/** A file written to storage: its key, size, SHA-256 and the first bytes for type sniffing. */
public record StoredObject(String key, long size, String sha256, byte[] head, int headLength) {
}
