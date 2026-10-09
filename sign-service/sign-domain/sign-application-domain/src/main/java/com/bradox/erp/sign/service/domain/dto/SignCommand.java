package com.bradox.erp.sign.service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Signing a template on screen. Each signature box takes a drawn PNG (base64) and each text box its typed text; name
 * and date boxes are filled by the server.
 */
public record SignCommand(@NotBlank @Size(max = 255) String signerName, List<Value> values) {

    public record Value(UUID fieldId, String text, String imagePng) {
    }
}
