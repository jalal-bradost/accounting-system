package com.bradox.erp.sign.service.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RequestFilter(List<String> statuses, String signer, UUID templateId, String recordModel, UUID recordId,
                            String q, Instant from, Instant to, boolean mineOnly, boolean needsAttention, int page, int size) {
}
