package com.bradox.erp.documents.service.domain.dto;

import java.util.UUID;

public record TagResponse(UUID id, UUID facetId, String name, String color, int sequence) {
}
