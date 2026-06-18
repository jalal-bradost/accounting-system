package com.jalaldeveloper.accountingsystem.platform.application.rest;

import com.jalaldeveloper.accountingsystem.platform.chatter.ChatterAttachmentStorage;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/media/chatter-attachments")
public class ChatterAttachmentMediaController {

    private final ChatterAttachmentStorage storage;

    public ChatterAttachmentMediaController(ChatterAttachmentStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> get(@PathVariable String filename) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.notFound().build();
        }
        String publicUrl = "/media/chatter-attachments/" + filename;
        return storage.openAsResource(publicUrl)
                .map(resource -> {
                    MediaType type = MediaType.APPLICATION_OCTET_STREAM;
                    try {
                        String detected = java.net.URLConnection.guessContentTypeFromName(filename);
                        if (detected != null) type = MediaType.parseMediaType(detected);
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
