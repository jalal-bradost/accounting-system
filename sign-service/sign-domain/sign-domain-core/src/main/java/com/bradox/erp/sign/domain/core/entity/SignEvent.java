package com.bradox.erp.sign.domain.core.entity;

import com.bradox.erp.sign.domain.core.rule.HashChain;
import com.bradox.erp.sign.domain.core.valueobject.EventType;

import java.time.Instant;
import java.util.UUID;

/** One append-only entry of the request's tamper-evident log (BR-SIG-08, SIG-08). There is no way to change one. */
public final class SignEvent {

    private final UUID id;
    private final UUID requestId;
    private final UUID signerItemId;
    private final EventType type;
    private final Instant occurredAt;
    private final String ip;
    private final String userAgent;
    private final String details;
    private final String prevHash;
    private final String eventHash;

    public SignEvent(UUID id, UUID requestId, UUID signerItemId, EventType type, Instant occurredAt, String ip,
                     String userAgent, String details, String prevHash, String eventHash) {
        this.id = id;
        this.requestId = requestId;
        this.signerItemId = signerItemId;
        this.type = type;
        this.occurredAt = occurredAt;
        this.ip = ip;
        this.userAgent = userAgent;
        this.details = details;
        this.prevHash = prevHash;
        this.eventHash = eventHash;
    }

    /** Chains a new event after {@code previousHash}. */
    public static SignEvent chain(UUID requestId, UUID signerItemId, EventType type, Instant at, String ip, String userAgent,
                                  String details, String previousHash) {
        UUID id = UUID.randomUUID();
        String prev = previousHash == null ? HashChain.GENESIS : previousHash;
        return new SignEvent(id, requestId, signerItemId, type, at, ip, userAgent, details, prev,
                HashChain.next(prev, canonicalOf(requestId, signerItemId, type, at, ip, userAgent, details)));
    }

    public static String canonicalOf(UUID requestId, UUID signerItemId, EventType type, Instant at, String ip, String ua,
                                     String details) {
        return HashChain.canonical(requestId.toString(), signerItemId == null ? null : signerItemId.toString(), type.name(),
                at.toString(), ip, ua, details);
    }

    /** True when this event's stored hash matches its content and the hash of its predecessor. */
    public boolean intact(String expectedPrevious) {
        String prev = expectedPrevious == null ? HashChain.GENESIS : expectedPrevious;
        return prev.equals(prevHash) && HashChain.next(prev, canonicalOf(requestId, signerItemId, type, occurredAt, ip, userAgent,
                details)).equals(eventHash);
    }

    public UUID getId() { return id; }
    public UUID getRequestId() { return requestId; }
    public UUID getSignerItemId() { return signerItemId; }
    public EventType getType() { return type; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public String getDetails() { return details; }
    public String getPrevHash() { return prevHash; }
    public String getEventHash() { return eventHash; }
}
