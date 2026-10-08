package com.bradox.erp.sign.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sign_event")
public class EventEntity {

    @Id
    private UUID id;
    @Column(name = "request_id", nullable = false)
    private UUID requestId;
    @Column(name = "signer_item_id")
    private UUID signerItemId;
    @Column(name = "event_type", nullable = false, length = 24)
    private String eventType;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "ip", length = 64)
    private String ip;
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    @Column(name = "details", length = 2000)
    private String details;
    @Column(name = "prev_hash", nullable = false, length = 64)
    private String prevHash;
    @Column(name = "event_hash", nullable = false, length = 64)
    private String eventHash;
    @Column(name = "seq", nullable = false)
    private long seq;

    public EventEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }
    public UUID getSignerItemId() { return signerItemId; }
    public void setSignerItemId(UUID signerItemId) { this.signerItemId = signerItemId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }
    public String getEventHash() { return eventHash; }
    public void setEventHash(String eventHash) { this.eventHash = eventHash; }
    public long getSeq() { return seq; }
    public void setSeq(long seq) { this.seq = seq; }
}
