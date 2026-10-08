package com.bradox.erp.sign.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sign_signer_item")
public class SignerEntity {

    @Id
    private UUID id;
    @Column(name = "request_id", nullable = false)
    private UUID requestId;
    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;
    @Column(name = "sequence", nullable = false)
    private int sequence;
    @Column(name = "approver_only", nullable = false)
    private boolean approverOnly;
    @Column(name = "partner_id")
    private UUID partnerId;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    @Column(name = "email", length = 255)
    private String email;
    @Column(name = "phone", length = 50)
    private String phone;
    @Column(name = "channel", nullable = false, length = 16)
    private String channel;
    @Column(name = "token_hash", length = 64)
    private String tokenHash;
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "viewed_at")
    private Instant viewedAt;
    @Column(name = "signed_at")
    private Instant signedAt;
    @Column(name = "refused_at")
    private Instant refusedAt;
    @Column(name = "refuse_reason", length = 1000)
    private String refuseReason;
    @Column(name = "signed_ip", length = 64)
    private String signedIp;
    @Column(name = "signed_user_agent", length = 500)
    private String signedUserAgent;
    @Column(name = "submission_digest", length = 64)
    private String submissionDigest;
    @Column(name = "link_issued", nullable = false)
    private boolean linkIssued;
    @Column(name = "operator_user_id")
    private UUID operatorUserId;
    @Column(name = "id_checked", nullable = false)
    private boolean idChecked;
    @Column(name = "id_note", length = 255)
    private String idNote;

    public SignerEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public boolean isApproverOnly() { return approverOnly; }
    public void setApproverOnly(boolean approverOnly) { this.approverOnly = approverOnly; }
    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(Instant tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getViewedAt() { return viewedAt; }
    public void setViewedAt(Instant viewedAt) { this.viewedAt = viewedAt; }
    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }
    public Instant getRefusedAt() { return refusedAt; }
    public void setRefusedAt(Instant refusedAt) { this.refusedAt = refusedAt; }
    public String getRefuseReason() { return refuseReason; }
    public void setRefuseReason(String refuseReason) { this.refuseReason = refuseReason; }
    public String getSignedIp() { return signedIp; }
    public void setSignedIp(String signedIp) { this.signedIp = signedIp; }
    public String getSignedUserAgent() { return signedUserAgent; }
    public void setSignedUserAgent(String signedUserAgent) { this.signedUserAgent = signedUserAgent; }
    public String getSubmissionDigest() { return submissionDigest; }
    public void setSubmissionDigest(String submissionDigest) { this.submissionDigest = submissionDigest; }
    public boolean isLinkIssued() { return linkIssued; }
    public void setLinkIssued(boolean linkIssued) { this.linkIssued = linkIssued; }
    public UUID getOperatorUserId() { return operatorUserId; }
    public void setOperatorUserId(UUID operatorUserId) { this.operatorUserId = operatorUserId; }
    public boolean isIdChecked() { return idChecked; }
    public void setIdChecked(boolean idChecked) { this.idChecked = idChecked; }
    public String getIdNote() { return idNote; }
    public void setIdNote(String idNote) { this.idNote = idNote; }
}
