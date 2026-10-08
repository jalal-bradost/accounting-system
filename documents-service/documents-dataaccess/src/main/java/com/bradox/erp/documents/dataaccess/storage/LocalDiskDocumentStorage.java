package com.bradox.erp.documents.dataaccess.storage;

import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.rule.FileTypePolicy;
import com.bradox.erp.documents.service.domain.ports.output.storage.DocumentStoragePort;
import com.bradox.erp.documents.service.domain.ports.output.storage.StoredObject;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stores files on local disk as {@code <root>/<first two key chars>/<key>}. Keys are random UUIDs
 * generated here, so no path ever contains user input. Writes go to a temporary file first and
 * are moved into place only when complete.
 */
@Component
public class LocalDiskDocumentStorage implements DocumentStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalDiskDocumentStorage.class);
    private static final Pattern KEY = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private final Path root;

    public LocalDiskDocumentStorage(@Value("${app.storage.documents.location:./data/documents}") String location) {
        this.root = Path.of(location).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
    }

    @Override
    public StoredObject store(InputStream content, long maxBytes) {
        String key = UUID.randomUUID().toString();
        Path target = pathFor(key);
        Path temp = root.resolve(key + ".part");
        MessageDigest digest = sha256();
        byte[] head = new byte[FileTypePolicy.SNIFF_BYTES];
        int headLength = 0;
        long total = 0;
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(temp, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = content.read(buffer)) != -1) {
                    if (headLength < head.length) {
                        int copy = Math.min(read, head.length - headLength);
                        System.arraycopy(buffer, 0, head, headLength, copy);
                        headLength += copy;
                    }
                    total += read;
                    if (total > maxBytes) {
                        throw new DocumentsDomainException("error.documents.fileTooLarge", new Object[]{maxBytes},
                                "The file is larger than the allowed " + (maxBytes / (1024 * 1024)) + " MB");
                    }
                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                }
            }
            if (total == 0) {
                throw new DocumentsDomainException("error.documents.fileEmpty", null, "The file is empty");
            }
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            return new StoredObject(key, total, HexFormat.of().formatHex(digest.digest()), head, headLength);
        } catch (IOException e) {
            deleteQuietly(temp);
            throw new UncheckedIOException("Failed to store document: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            deleteQuietly(temp);
            throw e;
        }
    }

    @Override
    public InputStream open(String key) {
        try {
            return Files.newInputStream(pathFor(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Stored file is missing: " + key, e);
        }
    }

    @Override
    public String copy(String key) {
        String newKey = UUID.randomUUID().toString();
        Path target = pathFor(newKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(pathFor(key), target, StandardCopyOption.COPY_ATTRIBUTES);
            return newKey;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to copy stored file: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(pathFor(key));
        } catch (IOException e) {
            log.warn("Could not delete stored document {}: {}", key, e.getMessage());
        }
    }

    private Path pathFor(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path path = root.resolve(key.substring(0, 2)).resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
