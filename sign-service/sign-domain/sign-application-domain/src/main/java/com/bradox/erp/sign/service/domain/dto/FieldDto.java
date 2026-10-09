package com.bradox.erp.sign.service.domain.dto;

import java.util.UUID;

/** A box on a template page; position and size are fractions (0 to 1) of the page. */
public record FieldDto(UUID id, int page, double x, double y, double width, double height, String type) {
}
