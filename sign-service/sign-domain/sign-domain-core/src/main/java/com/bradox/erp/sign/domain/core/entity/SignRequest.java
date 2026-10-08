package com.bradox.erp.sign.domain.core.entity;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.domain.core.rule.Tokens;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;
import com.bradox.erp.sign.domain.core.valueobject.RequestStatus;
import com.bradox.erp.sign.domain.core.valueobject.SignerStatus;
import com.bradox.erp.sign.domain.core.valueobject.SigningOrder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * One signing process for one document (SIG-02 to SIG-06). Owns its signers and their fields, and every rule about
 * who may sign when. Nothing outside this class changes a status.
 */
public class SignRequest {

    /** A value a signer submits for one field. */
    public record FieldValue(UUID fieldId, String text, Boolean bool, byte[] image) {
    }

    public enum SubmitResult { SIGNED, REPLAY }

    /** Download-only links last as long as the signed file is retained (D11). */
    static final long RETENTION_LINK_DAYS = 3650;

    private final UUID id;
    private final CompanyId companyId;
    private String reference;
    private UUID templateId;
    private UUID sourceDocumentId;
    private UUID sourceVersionId;
    private String sourceSha256;
    private int pageCount;
    private String name;
    private RequestStatus status = RequestStatus.DRAFT;
    private SigningOrder order;
    private String message;
    private Instant expiresAt;
    private Integer reminderEveryDays;
    private int reminderPrompts;
    private Instant lastReminderAt;
    private String recordModel;
    private UUID recordId;
    private UUID finalDocumentId;
    private String finalSha256;
    private Instant sentAt;
    private Instant completedAt;
    private Instant canceledAt;
    private String cancelReason;
    private boolean needsAttention;
    private String attentionReason;
    private int buildAttempts;
    private final Instant createdAt;
    private final String createdBy;
    private final UUID createdByUserId;
    private List<SignerItem> signers = new ArrayList<>();
    private List<RequestField> fields = new ArrayList<>();

    public SignRequest(UUID id, CompanyId companyId, String name, SigningOrder order, String message, UUID templateId,
                       UUID sourceDocumentId, UUID sourceVersionId, String sourceSha256, int pageCount, String recordModel,
                       UUID recordId, Integer reminderEveryDays, Instant createdAt, String createdBy, UUID createdByUserId) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.order = order == null ? SigningOrder.SEQUENTIAL : order;
        this.message = message;
        this.templateId = templateId;
        this.sourceDocumentId = sourceDocumentId;
        this.sourceVersionId = sourceVersionId;
        this.sourceSha256 = sourceSha256;
        this.pageCount = pageCount;
        this.recordModel = recordModel;
        this.recordId = recordId;
        this.reminderEveryDays = reminderEveryDays;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.createdByUserId = createdByUserId;
    }

    /** Loading from storage only. */
    public void restoreState(String reference, RequestStatus status, Instant expiresAt, int reminderPrompts, Instant lastReminderAt,
                             UUID finalDocumentId, String finalSha256, Instant sentAt, Instant completedAt, Instant canceledAt,
                             String cancelReason, boolean needsAttention, String attentionReason, int buildAttempts,
                             List<SignerItem> signers, List<RequestField> fields) {
        this.reference = reference;
        this.status = status;
        this.expiresAt = expiresAt;
        this.reminderPrompts = reminderPrompts;
        this.lastReminderAt = lastReminderAt;
        this.finalDocumentId = finalDocumentId;
        this.finalSha256 = finalSha256;
        this.sentAt = sentAt;
        this.completedAt = completedAt;
        this.canceledAt = canceledAt;
        this.cancelReason = cancelReason;
        this.needsAttention = needsAttention;
        this.attentionReason = attentionReason;
        this.buildAttempts = buildAttempts;
        this.signers = new ArrayList<>(signers);
        this.fields = new ArrayList<>(fields);
    }

    // ------------------------------------------------------------------ draft editing

    public void assignReference(String reference) {
        this.reference = reference;
    }

    public void editDraft(String name, SigningOrder order, String message, Integer reminderEveryDays, List<SignerItem> signers,
                          List<RequestField> fields) {
        requireStatus(RequestStatus.DRAFT, "Only a draft can be edited");
        if (name == null || name.isBlank()) {
            throw new SignDomainException("error.sign.request.name", null, "A request needs a name");
        }
        this.name = name.trim();
        this.order = order == null ? SigningOrder.SEQUENTIAL : order;
        this.message = message;
        this.reminderEveryDays = reminderEveryDays;
        this.signers = new ArrayList<>(signers);
        this.fields = new ArrayList<>(fields);
    }

    /** While still a draft the source is re-read at send time so the latest saved version is what gets pinned (D4). */
    public void repin(UUID versionId, String sha256, int pageCount) {
        requireStatus(RequestStatus.DRAFT, "Only a draft can be re-pinned");
        this.sourceVersionId = versionId;
        this.sourceSha256 = sha256;
        this.pageCount = pageCount;
    }

    /** BR-SIG-01, BR-SIG-02 and field sanity. Throws with a message naming the problem. */
    public void validateForSend() {
        if (signers.isEmpty()) {
            throw new SignDomainException("error.sign.request.noSigners", null, "Add at least one signer");
        }
        for (SignerItem s : signers) {
            if (s.getName() == null || s.getName().isBlank()) {
                throw new SignDomainException("error.sign.request.signerName", new Object[]{s.getRoleName()},
                        "Role " + s.getRoleName() + " has no signer name");
            }
            if (s.getEmail() != null && !s.getEmail().isBlank() && !s.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new SignDomainException("error.sign.request.signerEmail", new Object[]{s.getName()},
                        "The email of " + s.getName() + " is not valid");
            }
            boolean hasSignature = fields.stream().anyMatch(f -> f.getSignerItemId().equals(s.getId()) && f.isRequired()
                    && f.getType() == FieldType.SIGNATURE);
            if (!s.isApproverOnly() && !hasSignature) {
                throw new SignDomainException("error.sign.role.noSignature", new Object[]{s.getRoleName()},
                        "Role " + s.getRoleName() + " needs a required signature field");
            }
        }
        for (RequestField f : fields) {
            if (!f.getGeometry().within(pageCount)) {
                throw new SignDomainException("error.sign.field.pageRange", new Object[]{pageCount},
                        "A field is on a page the document does not have (it has " + pageCount + " pages)");
            }
        }
    }

    /** True when the same person appears for two roles (allowed, but the UI warns). */
    public boolean hasRepeatedSigner() {
        Map<String, Integer> seen = new HashMap<>();
        for (SignerItem s : signers) {
            String key = s.getPartnerId() != null ? "p" + s.getPartnerId()
                    : s.getUserId() != null ? "u" + s.getUserId()
                    : s.getEmail() != null && !s.getEmail().isBlank() ? "e" + s.getEmail().toLowerCase() : null;
            if (key != null && seen.merge(key, 1, Integer::sum) > 1) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ sending

    /**
     * Sends the request: pins it, opens the signers whose turn it is, and returns each one's link token, which is shown
     * once and never stored in plain (BR-SIG-03). Signers who must wait have no link yet.
     */
    public Map<UUID, String> send(Instant now, Instant expiresAt) {
        requireStatus(RequestStatus.DRAFT, "Only a draft can be sent");
        validateForSend();
        if (expiresAt == null || !expiresAt.isAfter(now)
                || expiresAt.isAfter(now.plusSeconds(SignRequestLimits.MAX_VALIDITY_DAYS * 86400L))) {
            throw new SignDomainException("error.sign.validity", new Object[]{SignRequestLimits.MAX_VALIDITY_DAYS},
                    "Validity must be between 1 and " + SignRequestLimits.MAX_VALIDITY_DAYS + " days");
        }
        this.expiresAt = expiresAt;
        this.sentAt = now;
        this.status = RequestStatus.SENT;
        List<SignerItem> ordered = ordered();
        Map<UUID, String> tokens = new LinkedHashMap<>();
        if (order == SigningOrder.PARALLEL) {
            for (SignerItem s : ordered) {
                s.setStatus(SignerStatus.PENDING);
                tokens.put(s.getId(), newToken(s));
            }
        } else {
            ordered.get(0).setStatus(SignerStatus.PENDING);
            tokens.put(ordered.get(0).getId(), newToken(ordered.get(0)));
        }
        return tokens;
    }

    /** A fresh link for a signer who has not finished; the previous link stops working (BR-SIG-03). */
    public String issueLink(UUID signerId) {
        requireLive();
        SignerItem s = signer(signerId);
        if (!s.isOpen()) {
            throw new SignDomainException("error.sign.signer.done", null, "This signer has already finished");
        }
        return newToken(s);
    }

    private String newToken(SignerItem s) {
        String token = Tokens.generate();
        s.issueToken(Tokens.hash(token), expiresAt);
        return token;
    }

    // ------------------------------------------------------------------ signing

    public boolean isLive() {
        return status == RequestStatus.SENT || status == RequestStatus.IN_PROGRESS;
    }

    /** The signer opened their link: the first open marks them viewed (SIG-03 #11). Returns true when it was the first. */
    public boolean markViewed(UUID signerId, Instant now) {
        SignerItem s = signer(signerId);
        if (s.getStatus() == SignerStatus.PENDING) {
            s.setStatus(SignerStatus.VIEWED);
            s.setViewedAt(now);
            return true;
        }
        return false;
    }

    /**
     * Records a signer's submission (SIG-03). The same payload again returns REPLAY and changes nothing (BR-SIG-04);
     * a different one after signing is an error. Throws without changing anything if a required field is empty
     * (BR-SIG-06) or a value belongs to someone else's field.
     */
    public SubmitResult submit(UUID signerId, List<FieldValue> values, String ip, String userAgent, Instant now) {
        SignerItem s = signer(signerId);
        String digest = digest(values);
        if (s.getStatus() == SignerStatus.SIGNED) {
            if (digest.equals(s.getSubmissionDigest())) {
                return SubmitResult.REPLAY;
            }
            throw new SignDomainException("error.sign.signer.alreadySigned", null, "This signer has already signed");
        }
        requireLive();
        requireTurn(s);
        Map<UUID, RequestField> mine = new HashMap<>();
        for (RequestField f : fields) {
            if (f.getSignerItemId().equals(signerId)) {
                mine.put(f.getId(), f);
            }
        }
        Map<UUID, FieldValue> byField = new HashMap<>();
        for (FieldValue v : values) {
            RequestField f = mine.get(v.fieldId());
            if (f == null) {
                throw new SignDomainException("error.sign.field.notYours", null, "A value was sent for a field that is not yours");
            }
            if (byField.put(v.fieldId(), v) != null) {
                throw new SignDomainException("error.sign.field.duplicate", null, "A field was filled twice");
            }
            validateValue(f, v);
        }
        for (RequestField f : mine.values()) {
            FieldValue v = byField.get(f.getId());
            boolean filled = v != null && switch (f.getType()) {
                case SIGNATURE, INITIALS -> v.image() != null && v.image().length > 0;
                case CHECKBOX -> v.bool() != null;
                case TEXT, DATE -> v.text() != null && !v.text().isBlank();
            };
            if (f.isRequired() && !filled) {
                throw new SignDomainException("error.sign.field.required",
                        new Object[]{f.getLabel() == null ? f.getType().name() : f.getLabel()},
                        "A required field is empty: " + (f.getLabel() == null ? f.getType().name() : f.getLabel()));
            }
        }
        for (RequestField f : mine.values()) {
            FieldValue v = byField.get(f.getId());
            if (v != null) {
                f.setValue(v.text(), v.bool(), v.image());
            }
        }
        s.signed(now, ip, userAgent, digest);
        if (status == RequestStatus.SENT) {
            status = RequestStatus.IN_PROGRESS;
        }
        activateNext(now);
        return SubmitResult.SIGNED;
    }

    private void validateValue(RequestField f, FieldValue v) {
        switch (f.getType()) {
            case SIGNATURE, INITIALS -> {
                if (v.image() != null && v.image().length > SignRequestLimits.MAX_SIGNATURE_IMAGE_BYTES) {
                    throw new SignDomainException("error.sign.image.tooBig", null, "The signature image is too large");
                }
            }
            case TEXT -> {
                if (v.text() != null && v.text().length() > SignRequestLimits.MAX_TEXT_LENGTH) {
                    throw new SignDomainException("error.sign.text.tooLong", new Object[]{SignRequestLimits.MAX_TEXT_LENGTH},
                            "Text can be at most " + SignRequestLimits.MAX_TEXT_LENGTH + " characters");
                }
            }
            case DATE -> {
                if (v.text() != null && !v.text().isBlank()) {
                    try {
                        LocalDate.parse(v.text().trim());
                    } catch (DateTimeParseException e) {
                        throw new SignDomainException("error.sign.date.invalid", null, "A date is not valid");
                    }
                }
            }
            case CHECKBOX -> { }
        }
    }

    /** Stable fingerprint of a payload, used to tell a retry from a different submission. */
    static String digest(List<FieldValue> values) {
        StringBuilder sb = new StringBuilder();
        values.stream().sorted(Comparator.comparing(v -> v.fieldId().toString())).forEach(v -> sb
                .append(v.fieldId()).append('|').append(v.text() == null ? "" : v.text()).append('|')
                .append(v.bool() == null ? "" : v.bool()).append('|')
                .append(v.image() == null ? "" : Tokens.sha256Hex(v.image())).append(';'));
        return Tokens.sha256Hex(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void requireTurn(SignerItem s) {
        if (order == SigningOrder.SEQUENTIAL && s.getStatus() == SignerStatus.WAITING) {
            throw new SignDomainException("error.sign.signer.notYourTurn", null, "Waiting for others to sign first");
        }
        if (!s.isActive()) {
            throw new SignDomainException("error.sign.signer.notActive", null, "This signer cannot sign now");
        }
    }

    /** In a sequential request, opens the next waiting signer once the one before has signed (BR-SIG-05). */
    private void activateNext(Instant now) {
        if (order != SigningOrder.SEQUENTIAL || !isLive()) {
            return;
        }
        boolean someoneActive = signers.stream().anyMatch(SignerItem::isActive);
        if (someoneActive) {
            return;
        }
        ordered().stream().filter(x -> x.getStatus() == SignerStatus.WAITING).findFirst().ifPresent(next -> next.setStatus(SignerStatus.PENDING));
    }

    /** The signer who just became active in a sequential request but has no link yet; the sender should share one. */
    public Optional<SignerItem> signerAwaitingLink() {
        return signers.stream().filter(x -> x.isActive() && !x.isLinkIssued()).findFirst();
    }

    public SignerItem refuse(UUID signerId, String reason, String ip, String userAgent, Instant now) {
        requireLive();
        SignerItem s = signer(signerId);
        requireTurn(s);
        if (reason == null || reason.isBlank()) {
            throw new SignDomainException("error.sign.refuse.reason", null, "Please say why you are refusing");
        }
        s.refused(now, reason.trim(), ip, userAgent);
        s.revokeToken();
        closeOthers(SignerStatus.CANCELED);
        status = RequestStatus.REFUSED;
        return s;
    }

    // ------------------------------------------------------------------ closing

    public void cancel(String reason, Instant now) {
        if (status == RequestStatus.COMPLETED || status == RequestStatus.CANCELED || status == RequestStatus.REFUSED
                || status == RequestStatus.EXPIRED) {
            throw new SignDomainException("error.sign.request.closed", null, "This request is already closed");
        }
        if (reason == null || reason.isBlank()) {
            throw new SignDomainException("error.sign.cancel.reason", null, "Please give a reason");
        }
        closeOthers(SignerStatus.CANCELED);
        status = RequestStatus.CANCELED;
        canceledAt = now;
        cancelReason = reason.trim();
    }

    /** Idempotent: an already expired request stays as it is. Returns true when this call changed it. */
    public boolean expire(Instant now) {
        if (!isLive() || expiresAt == null || expiresAt.isAfter(now)) {
            return false;
        }
        // Everyone has signed and only the final PDF is missing: it is complete in substance, never expire it.
        if (allSigned()) {
            return false;
        }
        closeOthers(SignerStatus.EXPIRED);
        status = RequestStatus.EXPIRED;
        return true;
    }

    private void closeOthers(SignerStatus to) {
        for (SignerItem s : signers) {
            if (s.isOpen()) {
                s.setStatus(to);
                s.revokeToken();
            }
        }
    }

    /** Extends before it lapses, within the cap counted from creation (SIG-06 #5). Existing links keep working. */
    public void extend(Instant newExpiry, Instant now) {
        requireLive();
        Instant cap = createdAt.plusSeconds(SignRequestLimits.MAX_VALIDITY_DAYS * 86400L);
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new SignDomainException("error.sign.request.expired", null, "This request has expired");
        }
        if (newExpiry == null || !newExpiry.isAfter(expiresAt) || newExpiry.isAfter(cap)) {
            throw new SignDomainException("error.sign.extend.range", new Object[]{SignRequestLimits.MAX_VALIDITY_DAYS},
                    "Pick a later date, no more than " + SignRequestLimits.MAX_VALIDITY_DAYS + " days after creation");
        }
        expiresAt = newExpiry;
        for (SignerItem s : signers) {
            if (s.isOpen() && s.getTokenHash() != null) {
                s.setTokenExpiresAt(newExpiry);
            }
        }
    }

    /** Replaces an open signer who has not looked at the document yet (SIG-04 #5). */
    public void replaceSigner(UUID signerId, UUID partnerId, UUID userId, String name, String email, String phone) {
        requireLive();
        SignerItem s = signer(signerId);
        if (!s.isOpen() || s.getViewedAt() != null || s.getStatus() == SignerStatus.VIEWED) {
            throw new SignDomainException("error.sign.replace.viewed", null, "This signer already opened the document");
        }
        if (name == null || name.isBlank()) {
            throw new SignDomainException("error.sign.request.signerName", new Object[]{s.getRoleName()},
                    "Role " + s.getRoleName() + " has no signer name");
        }
        s.replaceWith(partnerId, userId, name.trim(), email, phone);
        s.revokeToken();
    }

    // ------------------------------------------------------------------ reminders (SIG-06)

    /** True when the sender should be prompted to nudge a slow signer: interval passed, under the cap of 5. */
    public boolean reminderDue(Instant now) {
        if (!isLive() || reminderEveryDays == null || reminderEveryDays < 1 || reminderPrompts >= SignRequestLimits.MAX_REMINDER_PROMPTS) {
            return false;
        }
        Instant since = lastReminderAt != null ? lastReminderAt : (lastActivityAt() != null ? lastActivityAt() : sentAt);
        return since != null && !since.plusSeconds(reminderEveryDays * 86400L).isAfter(now);
    }

    public void recordReminder(Instant now) {
        requireLive();
        reminderPrompts++;
        lastReminderAt = now;
    }

    private Instant lastActivityAt() {
        return signers.stream().map(SignerItem::getSignedAt).filter(java.util.Objects::nonNull).max(Instant::compareTo).orElse(null);
    }

    // ------------------------------------------------------------------ completion

    public boolean allSigned() {
        return !signers.isEmpty() && signers.stream().allMatch(s -> s.getStatus() == SignerStatus.SIGNED);
    }

    /** All signed, final PDF not made yet. */
    public boolean awaitingFinalDocument() {
        return status == RequestStatus.IN_PROGRESS && allSigned() && finalDocumentId == null;
    }

    public void complete(UUID finalDocumentId, String finalSha256, Instant now) {
        if (status == RequestStatus.COMPLETED) {
            return;      // idempotent
        }
        if (!awaitingFinalDocument()) {
            throw new SignDomainException("error.sign.complete.notReady", null, "The request is not ready to complete");
        }
        this.finalDocumentId = finalDocumentId;
        this.finalSha256 = finalSha256;
        this.completedAt = now;
        this.status = RequestStatus.COMPLETED;
        this.needsAttention = false;
        this.attentionReason = null;
        // Signers keep their link for download only, for the retention period (BR-SIG-09).
        Instant keepUntil = now.plusSeconds(RETENTION_LINK_DAYS * 86400L);
        for (SignerItem s : signers) {
            if (s.getTokenHash() != null) {
                s.setTokenExpiresAt(keepUntil);
            }
        }
    }

    public void buildFailed(String reason) {
        buildAttempts++;
        attentionReason = reason;
    }

    public void flagAttention(String reason) {
        needsAttention = true;
        attentionReason = reason;
    }

    public boolean canRetryBuild() {
        return awaitingFinalDocument() && !needsAttention && buildAttempts < SignRequestLimits.MAX_BUILD_ATTEMPTS;
    }

    // ------------------------------------------------------------------ helpers

    public List<SignerItem> ordered() {
        return signers.stream().sorted(Comparator.comparingInt(SignerItem::getSequence)).toList();
    }

    public SignerItem signer(UUID id) {
        return signers.stream().filter(s -> s.getId().equals(id)).findFirst()
                .orElseThrow(() -> new SignDomainException("error.sign.signer.unknown", null, "Unknown signer"));
    }

    public Optional<SignerItem> signerByTokenHash(String hash) {
        return signers.stream().filter(s -> hash.equals(s.getTokenHash())).findFirst();
    }

    public List<RequestField> fieldsOf(UUID signerId) {
        return fields.stream().filter(f -> f.getSignerItemId().equals(signerId)).toList();
    }

    public int signedCount() {
        return (int) signers.stream().filter(s -> s.getStatus() == SignerStatus.SIGNED).count();
    }

    private void requireStatus(RequestStatus expected, String message) {
        if (status != expected) {
            throw new SignDomainException("error.sign.request.state", null, message);
        }
    }

    private void requireLive() {
        if (!isLive()) {
            throw new SignDomainException("error.sign.request.closed", null, "This request is closed");
        }
    }

    public UUID getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public String getReference() { return reference; }
    public UUID getTemplateId() { return templateId; }
    public UUID getSourceDocumentId() { return sourceDocumentId; }
    public UUID getSourceVersionId() { return sourceVersionId; }
    public String getSourceSha256() { return sourceSha256; }
    public int getPageCount() { return pageCount; }
    public String getName() { return name; }
    public RequestStatus getStatus() { return status; }
    public SigningOrder getOrder() { return order; }
    public String getMessage() { return message; }
    public Instant getExpiresAt() { return expiresAt; }
    public Integer getReminderEveryDays() { return reminderEveryDays; }
    public int getReminderPrompts() { return reminderPrompts; }
    public Instant getLastReminderAt() { return lastReminderAt; }
    public String getRecordModel() { return recordModel; }
    public UUID getRecordId() { return recordId; }
    public UUID getFinalDocumentId() { return finalDocumentId; }
    public String getFinalSha256() { return finalSha256; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCanceledAt() { return canceledAt; }
    public String getCancelReason() { return cancelReason; }
    public boolean isNeedsAttention() { return needsAttention; }
    public String getAttentionReason() { return attentionReason; }
    public int getBuildAttempts() { return buildAttempts; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public List<SignerItem> getSigners() { return List.copyOf(signers); }
    public List<RequestField> getFields() { return List.copyOf(fields); }
}
