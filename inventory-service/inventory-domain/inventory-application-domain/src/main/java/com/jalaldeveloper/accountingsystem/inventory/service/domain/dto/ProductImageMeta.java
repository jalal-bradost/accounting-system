package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

/**
 * Image metadata stored on {@code inv_product} (not part of the domain aggregate).
 */
public record ProductImageMeta(String imageUrl, String contentType) {}
