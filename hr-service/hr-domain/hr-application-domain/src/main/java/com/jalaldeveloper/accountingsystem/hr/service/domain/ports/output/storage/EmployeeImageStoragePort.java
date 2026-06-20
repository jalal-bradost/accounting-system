package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeImageStoragePort {

    record StoredImage(String publicUrl, String contentType) {}

    StoredImage store(UUID companyId, UUID employeeId, String contentType, long size, InputStream content);

    void deleteIfPresent(String publicUrl);

    Optional<Resource> openAsResource(String publicUrl);
}
