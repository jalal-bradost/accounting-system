package com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

public interface PartnerImageStoragePort {

    record StoredImage(String publicUrl, String contentType) {}

    StoredImage store(UUID companyId, UUID partnerId, String contentType, long size, InputStream content);

    void deleteIfPresent(String publicUrl);

    Optional<Resource> openAsResource(String publicUrl);
}
