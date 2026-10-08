package com.bradox.erp.sign.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.domain.core.entity.RequestField;
import com.bradox.erp.sign.domain.core.entity.SignRequest;
import com.bradox.erp.sign.domain.core.entity.SignerItem;
import com.bradox.erp.sign.domain.core.rule.Tokens;
import com.bradox.erp.sign.domain.core.valueobject.EventType;
import com.bradox.erp.sign.service.domain.ports.output.PdfPort;
import com.bradox.erp.sign.service.domain.ports.output.PdfPort.Placement;
import com.bradox.erp.sign.service.domain.ports.output.SignNotificationPort;
import com.bradox.erp.sign.service.domain.ports.output.SignedDocumentSinkPort;
import com.bradox.erp.sign.service.domain.ports.output.SourceDocumentPort;
import com.bradox.erp.sign.service.domain.ports.output.SourceDocumentPort.SourceDoc;
import com.bradox.erp.sign.service.domain.ports.output.repository.RequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the final PDF with its certificate once everyone has signed (SIG-05). The server draws the values from stored
 * data (D3); the browser's rendering is never the legal document. Safe to call twice: a finished request is skipped.
 */
@Component
class RequestFinalizer {

    private static final Logger log = LoggerFactory.getLogger(RequestFinalizer.class);
    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");
    private static final DateTimeFormatter LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RequestRepository requests;
    private final SourceDocumentPort sources;
    private final SignedDocumentSinkPort sink;
    private final PdfPort pdf;
    private final SignNotificationPort notifications;
    private final SignRecorder recorder;
    private final SignAccess access;
    private final TransactionOperations tx;
    private final ZoneId zone;
    private final int retentionYears;

    RequestFinalizer(RequestRepository requests, SourceDocumentPort sources, SignedDocumentSinkPort sink, PdfPort pdf,
                     SignNotificationPort notifications, SignRecorder recorder, SignAccess access, TransactionOperations tx,
                     @Value("${app.sign.time-zone:Asia/Baghdad}") String zone,
                     @Value("${app.sign.retention-years:10}") int retentionYears) {
        this.requests = requests;
        this.sources = sources;
        this.sink = sink;
        this.pdf = pdf;
        this.notifications = notifications;
        this.recorder = recorder;
        this.access = access;
        this.tx = tx;
        this.zone = ZoneId.of(zone);
        this.retentionYears = retentionYears;
    }

    /** Never throws: a failure is recorded on the request and retried by the job (up to 3 times). */
    void finalizeRequest(UUID requestId) {
        try {
            tx.executeWithoutResult(status -> doFinalize(requestId));
        } catch (RuntimeException e) {
            log.warn("Final PDF for sign request {} failed: {}", requestId, e.toString());
            try {
                tx.executeWithoutResult(status -> requests.findAny(requestId).ifPresent(r -> {
                    r.buildFailed(String.valueOf(e.getMessage()));
                    requests.save(r);
                    recorder.event(r.getId(), null, EventType.ATTENTION, access.now(), null, null,
                            "Final PDF could not be built (attempt " + r.getBuildAttempts() + ")");
                }));
            } catch (RuntimeException inner) {
                log.error("Could not record the failed build of {}", requestId, inner);
            }
        }
    }

    private void doFinalize(UUID requestId) {
        SignRequest r = requests.findAny(requestId).orElse(null);
        if (r == null || !r.awaitingFinalDocument()) {
            return;
        }
        CompanyId companyId = r.getCompanyId();
        SourceDoc source = sources.readPinned(companyId, r.getSourceDocumentId(), r.getSourceVersionId());
        if (!source.sha256().equals(r.getSourceSha256())) {          // BR-SIG-07
            r.flagAttention("The source document no longer matches what was sent for signature");
            requests.save(r);
            recorder.event(r.getId(), null, EventType.ATTENTION, access.now(), null, null, "Source document hash mismatch");
            notifyOwner(r, "Signed document needs attention: " + r.getName(),
                    "Everyone signed, but the original document changed, so the final PDF was not made. Signer values are kept.");
            return;
        }
        List<Placement> placements = new ArrayList<>();
        for (RequestField f : r.getFields()) {
            if (f.isFilled()) {
                var g = f.getGeometry();
                placements.add(new Placement(g.page(), g.x(), g.y(), g.width(), g.height(), f.getType(), f.getValueText(),
                        f.getValueBool(), f.getValueImage()));
            }
        }
        byte[] signed = pdf.applyValues(source.bytes(), placements);
        String contentSha = Tokens.sha256Hex(signed);
        Instant now = access.now();
        List<PdfPort.CertificateSigner> signers = new ArrayList<>();
        for (SignerItem s : r.ordered()) {
            signers.add(new PdfPort.CertificateSigner(s.getName(), s.getEmail(), s.getRoleName(),
                    UTC_FORMAT.format(s.getSignedAt().atZone(java.time.ZoneOffset.UTC)),
                    LOCAL_FORMAT.format(s.getSignedAt().atZone(zone)) + " " + zone.getId(), s.getSignedIp(), s.getChannel().name()));
        }
        byte[] finalPdf = pdf.appendCertificate(signed, new PdfPort.Certificate(r.getReference(), r.getName(), r.getSourceSha256(),
                contentSha, r.getId().toString(), zone.getId(), UTC_FORMAT.format(now.atZone(java.time.ZoneOffset.UTC)), signers));
        String finalSha = Tokens.sha256Hex(finalPdf);
        LocalDate retainUntil = LocalDate.now(Clock.system(zone)).plusYears(retentionYears);
        UUID documentId = sink.store(companyId, (r.getReference() + " " + r.getName()).replaceAll("[^\\p{L}\\p{N} ._-]", "_")
                + " (signed).pdf", finalPdf, r.getId(), r.getReference(), r.getRecordModel(), r.getRecordId(), retainUntil);
        r.complete(documentId, finalSha, now);
        requests.save(r);
        recorder.event(r.getId(), null, EventType.COMPLETED, now, null, null, "Final SHA-256 " + finalSha);
        recorder.audit(companyId, r.getId(), "Signature request completed", Map.of("finalSha256", finalSha));
        notifyOwner(r, "Signed: " + r.getName(), "Everyone has signed " + r.getName() + " (" + r.getReference()
                + "). The signed PDF is stored in Documents.");
    }

    void notifyOwner(SignRequest r, String subject, String body) {
        if (r.getCreatedByUserId() == null) {
            return;
        }
        try {
            notifications.notifyUser(r.getCompanyId(), r.getCreatedByUserId(), r.getId(), subject, body);
        } catch (RuntimeException e) {
            log.warn("Sign notification failed for {}: {}", r.getId(), e.toString());
        }
    }
}
