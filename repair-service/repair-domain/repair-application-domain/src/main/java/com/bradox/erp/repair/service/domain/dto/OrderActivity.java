package com.bradox.erp.repair.service.domain.dto;

import java.time.Instant;
import java.util.UUID;

/** One order that has repair work recorded, until the maintenance order list (RCP) exists. */
public record OrderActivity(UUID orderId, int lines, int findings, boolean inspected, boolean signedOff, Instant lastActivity) {
}
