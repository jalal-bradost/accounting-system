package com.jalaldeveloper.accountingsystem.hr.dataaccess.storage;

import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.storage.EmployeeImageStoragePort;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class EmployeeImageStorageAdapter implements EmployeeImageStoragePort {

    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private static final Map<String, String> EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final Path root;
    private final String publicBasePath;
    private final long maxBytes;

    public EmployeeImageStorageAdapter(
            @Value("${app.storage.employee-images.location:./data/employee-images}") String location,
            @Value("${app.storage.employee-images.public-base-path:/media/employee-images}") String publicBasePath,
            @Value("${app.storage.employee-images.max-bytes:5242880}") long maxBytes) {
        this.root = Path.of(location).toAbsolutePath().normalize();
        this.publicBasePath = publicBasePath.endsWith("/")
                ? publicBasePath.substring(0, publicBasePath.length() - 1)
                : publicBasePath;
        this.maxBytes = maxBytes;
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
    }

    @Override
    public StoredImage store(UUID companyId, UUID employeeId, String contentType, long size, InputStream content) {
        if (companyId == null || employeeId == null) {
            throw new HrDomainException("companyId and employeeId required for image storage");
        }
        String normalized = normalizeContentType(contentType);
        if (!ALLOWED.contains(normalized)) {
            throw new HrDomainException("Unsupported image type: " + contentType);
        }
        if (size <= 0 || size > maxBytes) {
            throw new HrDomainException("Image must be between 1 byte and " + maxBytes + " bytes");
        }

        String filename = companyId + "_" + employeeId + "_" + UUID.randomUUID() + EXT.get(normalized);
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new HrDomainException("Invalid image path");
        }

        try (InputStream in = content) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new HrDomainException("Failed to store employee image: " + ex.getMessage());
        }

        try {
            if (Files.size(target) > maxBytes) {
                Files.deleteIfExists(target);
                throw new HrDomainException("Image exceeds maximum size of " + maxBytes + " bytes");
            }
        } catch (IOException ex) {
            throw new HrDomainException("Failed to verify employee image size");
        }

        return new StoredImage(publicBasePath + "/" + filename, normalized);
    }

    @Override
    public void deleteIfPresent(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) return;
        resolveFilename(publicUrl).ifPresent(filename -> {
            try {
                Files.deleteIfExists(root.resolve(filename).normalize());
            } catch (IOException ex) {
                throw new HrDomainException("Failed to delete employee image: " + ex.getMessage());
            }
        });
    }

    @Override
    public Optional<Resource> openAsResource(String publicUrl) {
        return resolveFilename(publicUrl)
                .map(filename -> root.resolve(filename).normalize())
                .filter(path -> path.startsWith(root) && Files.isRegularFile(path))
                .map(FileSystemResource::new);
    }

    private Optional<String> resolveFilename(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) return Optional.empty();
        String prefix = publicBasePath + "/";
        if (!publicUrl.startsWith(prefix)) return Optional.empty();
        String filename = publicUrl.substring(prefix.length());
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return Optional.empty();
        }
        return Optional.of(filename);
    }

    private static String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) return "";
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }
}
