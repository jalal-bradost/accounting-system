package com.bradox.erp.sign.service.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code signedIp} and {@code signedUserAgent} are filled only for people with sign.audit.view (privacy). */
public record SignerResponse(UUID id, String roleName, int sequence, boolean approverOnly, String name, String email, String phone,
                             UUID partnerId, UUID userId, String status, String channel, boolean linkIssued, Instant viewedAt,
                             Instant signedAt, Instant refusedAt, String refuseReason, String signedIp, String signedUserAgent) {
}
