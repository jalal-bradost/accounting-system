package com.bradox.erp.sign.service.domain.dto;

import java.util.UUID;

/** A field of the person who opened the link. Others' fields are never sent (D9). */
public record PublicFieldResponse(UUID id, int page, double x, double y, double width, double height, String type,
                                  boolean required, String label, String placeholder, String prefill) {
}
