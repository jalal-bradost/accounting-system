package com.jalaldeveloper.accountingsystem.inventory.application.rest;

import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.storage.ProductImageStoragePort;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves stored product images. Filenames are unguessable UUIDs; no auth required for GET.
 */
@RestController
@RequestMapping("/media/product-images")
public class ProductImageMediaController {

    private final ProductImageStoragePort imageStorage;

    public ProductImageMediaController(ProductImageStoragePort imageStorage) {
        this.imageStorage = imageStorage;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> get(@PathVariable String filename) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.notFound().build();
        }
        String publicUrl = "/media/product-images/" + filename;
        return imageStorage.openAsResource(publicUrl)
                .map(resource -> {
                    MediaType type = MediaType.APPLICATION_OCTET_STREAM;
                    try {
                        if (resource.getURL() != null) {
                            String detected = java.net.URLConnection.guessContentTypeFromName(filename);
                            if (detected != null) type = MediaType.parseMediaType(detected);
                        }
                    } catch (Exception ignored) {
                        // use default
                    }
                    return ResponseEntity.ok()
                            .contentType(type)
                            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                            .body(resource);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
