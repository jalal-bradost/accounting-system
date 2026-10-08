package com.bradox.erp.sign.domain.core.rule;

import java.nio.charset.StandardCharsets;

/** Tamper evidence for the event log (SIG-08): each event hash covers the previous hash and the event's own content. */
public final class HashChain {

    public static final String GENESIS = "0".repeat(64);

    private HashChain() {
    }

    /** {@code canonical} is the event serialised in a fixed field order, so the same event always hashes the same. */
    public static String next(String previousHash, String canonical) {
        String prev = previousHash == null || previousHash.isBlank() ? GENESIS : previousHash;
        return Tokens.sha256Hex((prev + "|" + canonical).getBytes(StandardCharsets.UTF_8));
    }

    public static String canonical(String requestId, String signerItemId, String type, String occurredAt, String ip,
                                   String userAgent, String details) {
        return String.join("\u001f", requestId, signerItemId == null ? "" : signerItemId, type, occurredAt,
                ip == null ? "" : ip, userAgent == null ? "" : userAgent, details == null ? "" : details);
    }
}
