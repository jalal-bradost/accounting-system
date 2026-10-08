package com.bradox.erp.sign.service.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code ip} and {@code userAgent} are filled only for people with sign.audit.view. */
public record EventResponse(UUID id, String type, Instant occurredAt, UUID signerId, String signerName, String ip,
                            String userAgent, String details) {
}
