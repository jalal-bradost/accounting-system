package com.bradox.erp.documents.service.domain.ports.output.storage;

import java.io.InputStream;

/** Where file bytes live. The local disk adapter can be replaced by S3 without touching the domain. */
public interface DocumentStoragePort {

    /**
     * Streams the content to storage under a random key while counting bytes and hashing.
     * Fails when the content is empty or longer than {@code maxBytes}. Never buffers the whole file.
     */
    StoredObject store(InputStream content, long maxBytes);

    InputStream open(String key);

    /** Copies the stored file to a new key, used when a version is restored. */
    String copy(String key);

    /** Removes a stored file. Missing files are ignored. */
    void delete(String key);
}
