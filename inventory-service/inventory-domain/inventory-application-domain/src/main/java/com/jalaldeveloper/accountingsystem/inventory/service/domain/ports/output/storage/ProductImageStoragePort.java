package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.storage;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.Resource;

/**
 * Stores product image binaries on the local filesystem and exposes stable public URLs.
 */
public interface ProductImageStoragePort {

    StoredImage store(UUID companyId, UUID productId, String contentType, long size, InputStream content);

    void deleteIfPresent(String publicUrl);

    Optional<Resource> openAsResource(String publicUrl);

    record StoredImage(String publicUrl, String contentType) {}
}
