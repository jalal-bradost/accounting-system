package com.bradox.erp.sign.service.domain.dto;

import java.util.UUID;

public record RoleDto(UUID id, String name, String color, int sequence, boolean approverOnly, UUID defaultPartnerId) {
}
