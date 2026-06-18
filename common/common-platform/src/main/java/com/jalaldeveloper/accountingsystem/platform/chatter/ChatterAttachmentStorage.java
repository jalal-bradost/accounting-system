package com.jalaldeveloper.accountingsystem.platform.chatter;

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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ChatterAttachmentStorage {

    private static final Set<String> ALLOWED = Set.of(
            "application/pdf",
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "text/plain", "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final Path root;
    private final String publicBasePath;
    private final long maxBytes;

    public ChatterAttachmentStorage(
            @Value("${app.storage.chatter-attachments.location:./data/chatter-attachments}") String location,
            @Value("${app.storage.chatter-attachments.public-base-path:/media/chatter-attachments}") String publicBasePath,
            @Value("${app.storage.chatter-attachments.max-bytes:10485760}") long maxBytes) {
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

    public StoredFile store(UUID companyId, String modelName, UUID recordId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
        }
        if (file.getSize() <= 0 || file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File must be between 1 byte and " + maxBytes + " bytes");
        }

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        if (original.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        String ext = extensionFor(original, contentType);
        String storedName = companyId + "_" + sanitize(modelName) + "_" + recordId + "_"
                + UUID.randomUUID() + ext;
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store attachment: " + ex.getMessage());
        }

        return new StoredFile(original, contentType, file.getSize(), publicBasePath + "/" + storedName);
    }

    public Optional<Resource> openAsResource(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(publicBasePath + "/")) {
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

    public void deleteByPublicUrl(String publicUrl) {
        openAsResource(publicUrl).ifPresent(resource -> {
            try {
                Path path = resource.getFile().toPath();
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // best effort
            }
        });
    }

    private static String sanitize(String modelName) {
        return modelName.replace('.', '_').replace('/', '_');
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) return "application/octet-stream";
        return contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
    }

    private static String extensionFor(String original, String contentType) {
        int dot = original.lastIndexOf('.');
        if (dot > 0 && dot < original.length() - 1) {
            return original.substring(dot).toLowerCase(Locale.ROOT);
        }
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "text/plain" -> ".txt";
            case "text/csv" -> ".csv";
            default -> "";
        };
    }

    public record StoredFile(String fileName, String contentType, long fileSize, String publicUrl) {}
}
