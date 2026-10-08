package com.bradox.erp.sign.domain.core.entity;

import com.bradox.erp.sign.domain.core.valueobject.Channel;
import com.bradox.erp.sign.domain.core.valueobject.SignerStatus;

import java.time.Instant;
import java.util.UUID;

/** One signer's part of a request, with its own link token, status and evidence. */
public class SignerItem {

    private final UUID id;
    private String roleName;
    private int sequence;
    private boolean approverOnly;
    private UUID partnerId;
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private Channel channel;
    private String tokenHash;
    private Instant tokenExpiresAt;
    private SignerStatus status;
    private Instant viewedAt;
    private Instant signedAt;
    private Instant refusedAt;
    private String refuseReason;
    private String signedIp;
    private String signedUserAgent;
    private String submissionDigest;
    private boolean linkIssued;
    private UUID operatorUserId;
    private boolean idChecked;
    private String idNote;

    public SignerItem(UUID id, String roleName, int sequence, boolean approverOnly, UUID partnerId, UUID userId, String name,
                      String email, String phone, Channel channel) {
        this.id = id;
        this.roleName = roleName;
        this.sequence = sequence;
        this.approverOnly = approverOnly;
        this.partnerId = partnerId;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.channel = channel == null ? Channel.LINK : channel;
        this.status = SignerStatus.WAITING;
    }

    /** Used only when loading from storage. */
    public void restoreState(String tokenHash, Instant tokenExpiresAt, SignerStatus status, Instant viewedAt, Instant signedAt,
                             Instant refusedAt, String refuseReason, String signedIp, String signedUserAgent,
                             String submissionDigest, boolean linkIssued, UUID operatorUserId, boolean idChecked, String idNote) {
        this.tokenHash = tokenHash;
        this.tokenExpiresAt = tokenExpiresAt;
        this.status = status;
        this.viewedAt = viewedAt;
        this.signedAt = signedAt;
        this.refusedAt = refusedAt;
        this.refuseReason = refuseReason;
        this.signedIp = signedIp;
        this.signedUserAgent = signedUserAgent;
        this.submissionDigest = submissionDigest;
        this.linkIssued = linkIssued;
        this.operatorUserId = operatorUserId;
        this.idChecked = idChecked;
        this.idNote = idNote;
    }

    public boolean isOpen() {
        return status == SignerStatus.WAITING || status == SignerStatus.PENDING || status == SignerStatus.VIEWED;
    }

    public boolean isActive() {
        return status == SignerStatus.PENDING || status == SignerStatus.VIEWED;
    }

    void setStatus(SignerStatus s) { this.status = s; }
    void issueToken(String hash, Instant expiresAt) { this.tokenHash = hash; this.tokenExpiresAt = expiresAt; this.linkIssued = true; }
    void revokeToken() { this.tokenHash = null; this.linkIssued = false; }
    void setViewedAt(Instant t) { this.viewedAt = t; }
    void signed(Instant t, String ip, String ua, String digest) { this.signedAt = t; this.signedIp = ip; this.signedUserAgent = ua; this.submissionDigest = digest; this.status = SignerStatus.SIGNED; }
    void refused(Instant t, String reason, String ip, String ua) { this.refusedAt = t; this.refuseReason = reason; this.signedIp = ip; this.signedUserAgent = ua; this.status = SignerStatus.REFUSED; }
    void replaceWith(UUID partnerId, UUID userId, String name, String email, String phone) { this.partnerId = partnerId; this.userId = userId; this.name = name; this.email = email; this.phone = phone; }
    void handedOver(UUID operator, boolean idChecked, String idNote) { this.operatorUserId = operator; this.idChecked = idChecked; this.idNote = idNote; }
    void setTokenExpiresAt(Instant t) { this.tokenExpiresAt = t; }

    public UUID getId() { return id; }
    public String getRoleName() { return roleName; }
    public int getSequence() { return sequence; }
    public boolean isApproverOnly() { return approverOnly; }
    public UUID getPartnerId() { return partnerId; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Channel getChannel() { return channel; }
    public String getTokenHash() { return tokenHash; }
    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public SignerStatus getStatus() { return status; }
    public Instant getViewedAt() { return viewedAt; }
    public Instant getSignedAt() { return signedAt; }
    public Instant getRefusedAt() { return refusedAt; }
    public String getRefuseReason() { return refuseReason; }
    public String getSignedIp() { return signedIp; }
    public String getSignedUserAgent() { return signedUserAgent; }
    public String getSubmissionDigest() { return submissionDigest; }
    public boolean isLinkIssued() { return linkIssued; }
    public UUID getOperatorUserId() { return operatorUserId; }
    public boolean isIdChecked() { return idChecked; }
    public String getIdNote() { return idNote; }
}
