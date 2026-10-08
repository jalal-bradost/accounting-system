package com.bradox.erp.sign.service.domain.dto;

import java.util.UUID;

/** {@code imagePng} is base64 (optionally a data URL) of a PNG, for signature and initials fields. */
public record PublicValue(UUID fieldId, String text, Boolean bool, String imagePng) {
}
