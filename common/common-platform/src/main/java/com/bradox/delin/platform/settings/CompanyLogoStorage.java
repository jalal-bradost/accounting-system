package com.bradox.delin.platform.settings;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
public class CompanyLogoStorage {

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

    public CompanyLogoStorage(
            @Value("${app.storage.company-logos.location:./data/company-logos}") String location,
            @Value("${app.storage.company-logos.public-base-path:/media/company-logos}") String publicBasePath,
            @Value("${app.storage.company-logos.max-bytes:5242880}") long maxBytes) {
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

    public String publicBasePath() {
        return publicBasePath;
    }

    public StoredImage store(UUID companyId, MultipartFile file) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId required for logo storage");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo file is required");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image type: " + file.getContentType());
        }
        if (file.getSize() <= 0 || file.getSize() > maxBytes) {
            throw new IllegalArgumentException("Image must be between 1 byte and " + maxBytes + " bytes");
        }

        String filename = companyId + "_" + UUID.randomUUID() + EXT.get(contentType);
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid logo path");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store company logo: " + ex.getMessage());
        }

        try {
            if (Files.size(target) > maxBytes) {
                Files.deleteIfExists(target);
                throw new IllegalArgumentException("Image exceeds maximum size of " + maxBytes + " bytes");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to verify company logo size");
        }

        return new StoredImage(publicBasePath + "/" + filename, contentType);
    }

    public void deleteIfPresent(String publicUrl) {
        if (!StringUtils.hasText(publicUrl) || !publicUrl.startsWith(publicBasePath + "/")) {
            return;
        }
        openAsResource(publicUrl).ifPresent(resource -> {
            try {
                Files.deleteIfExists(resource.getFile().toPath());
            } catch (IOException ignored) {
                // best effort
            }
        });
    }

    public Optional<Resource> openAsResource(String publicUrl) {
        if (!StringUtils.hasText(publicUrl) || !publicUrl.startsWith(publicBasePath + "/")) {
            return Optional.empty();
        }
        String filename = publicUrl.substring(publicBasePath.length() + 1);
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return Optional.empty();
        }
        Path path = root.resolve(filename).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(path));
    }

    private static String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) return "";
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    public record StoredImage(String publicUrl, String contentType) {}
}
