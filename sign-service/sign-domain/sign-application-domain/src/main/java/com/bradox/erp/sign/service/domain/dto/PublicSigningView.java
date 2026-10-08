package com.bradox.erp.sign.service.domain.dto;

import java.util.List;

/**
 * What the signing page needs. {@code state}: READY (can sign), WAITING (not your turn: no content is sent), DONE (you
 * signed, others pending) or COMPLETED (finished, download available). Anything else is a neutral 404.
 */
public record PublicSigningView(String state, String requestName, String message, String signerName, String roleName,
                                int pageCount, int signedCount, int signerCount, List<PublicFieldResponse> fields,
                                boolean downloadAvailable, String channel) {
}
