package com.bradox.erp.sign.service.domain.dto;

import java.util.UUID;

/** A field box. {@code roleId} is a template role id, or for a one-off request the key of the signer it belongs to. */
public record FieldDto(UUID id, String roleKey, int page, double x, double y, double width, double height, String type,
                       boolean required, String label, String placeholder, String autoFill) {
}
