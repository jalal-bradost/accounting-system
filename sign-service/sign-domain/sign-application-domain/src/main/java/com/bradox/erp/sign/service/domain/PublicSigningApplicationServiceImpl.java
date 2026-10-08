package com.bradox.erp.sign.service.domain;

import com.bradox.erp.sign.domain.core.entity.RequestField;
import com.bradox.erp.sign.domain.core.entity.SignRequest;
import com.bradox.erp.sign.domain.core.entity.SignRequest.FieldValue;
import com.bradox.erp.sign.domain.core.entity.SignRequest.SubmitResult;
import com.bradox.erp.sign.domain.core.entity.SignerItem;
import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.domain.core.rule.Tokens;
import com.bradox.erp.sign.domain.core.valueobject.EventType;
import com.bradox.erp.sign.domain.core.valueobject.RequestStatus;
import com.bradox.erp.sign.domain.core.valueobject.SignerStatus;
import com.bradox.erp.sign.service.domain.dto.FileContent;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.PublicFieldResponse;
import com.bradox.erp.sign.service.domain.dto.PublicRefuseCommand;
import com.bradox.erp.sign.service.domain.dto.PublicResultResponse;
import com.bradox.erp.sign.service.domain.dto.PublicSigningView;
import com.bradox.erp.sign.service.domain.dto.PublicSubmitCommand;
import com.bradox.erp.sign.service.domain.dto.PublicValue;
import com.bradox.erp.sign.service.domain.ports.input.PublicSigningApplicationService;
import com.bradox.erp.sign.service.domain.ports.output.PdfPort;
import com.bradox.erp.sign.service.domain.ports.output.SignedDocumentSinkPort;
import com.bradox.erp.sign.service.domain.ports.output.SourceDocumentPort;
import com.bradox.erp.sign.service.domain.ports.output.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * The signing page's backend (SIG-03). The link token is the only credential. Every refusal looks the same, a plain 404, so
 * a bad, expired, revoked or closed link reveals nothing about which it was.
 */
@Service
class PublicSigningApplicationServiceImpl implements PublicSigningApplicationService {

    private static final float VIEW_SCALE = 1.5f;

    private final RequestRepository requests;
    private final SourceDocumentPort sources;
    private final SignedDocumentSinkPort sink;
    private final PdfPort pdf;
    private final PageCache pageCache;
    private final PublicRateLimiter limiter;
    private final RequestFinalizer finalizer;
    private final SignRecorder recorder;
    private final SignAccess access;
    private final TransactionOperations tx;
    private final ZoneId zone;

    PublicSigningApplicationServiceImpl(RequestRepository requests, SourceDocumentPort sources, SignedDocumentSinkPort sink, PdfPort pdf,
                                        PageCache pageCache, PublicRateLimiter limiter, RequestFinalizer finalizer,
                                        SignRecorder recorder, SignAccess access, TransactionOperations tx,
                                        @Value("${app.sign.time-zone:Asia/Baghdad}") String zone) {
        this.requests = requests;
        this.sources = sources;
        this.sink = sink;
        this.pdf = pdf;
        this.pageCache = pageCache;
        this.limiter = limiter;
        this.finalizer = finalizer;
        this.recorder = recorder;
        this.access = access;
        this.tx = tx;
        this.zone = ZoneId.of(zone);
    }

    private record Resolved(SignRequest request, SignerItem signer, String hash) {
    }

    // ------------------------------------------------------------------ reads

    @Override
    public PublicSigningView open(String token, String ip, String userAgent) {
        return tx.execute(status -> {
            Resolved res = resolve(token, ip);
            SignRequest r = res.request();
            SignerItem s = res.signer();
            Instant now = access.now();
            recorder.event(r.getId(), s.getId(), EventType.OPENED, now, ip, userAgent, "Link opened");
            if (r.isLive() && r.markViewed(s.getId(), now)) {
                requests.save(r);
            }
            return view(r, r.signer(s.getId()));
        });
    }

    @Override
    public PageImage page(String token, int page) {
        Resolved res = resolve(token, null);
        SignRequest r = res.request();
        if (res.signer().getStatus() == SignerStatus.WAITING || page < 1 || page > r.getPageCount()) {
            throw notAvailable();
        }
        return pageCache.get(r.getSourceSha256(), page, VIEW_SCALE, () -> pdf.render(
                sources.readPinned(r.getCompanyId(), r.getSourceDocumentId(), r.getSourceVersionId()).bytes(), page, VIEW_SCALE));
    }

    @Override
    public FileContent download(String token, String ip, String userAgent) {
        Resolved res = resolve(token, ip);
        SignRequest r = res.request();
        if (r.getStatus() != RequestStatus.COMPLETED || r.getFinalDocumentId() == null) {
            throw notAvailable();
        }
        tx.executeWithoutResult(status -> recorder.event(r.getId(), res.signer().getId(), EventType.OPENED, access.now(), ip, userAgent,
                "Signed document downloaded"));
        String name = ((r.getReference() == null ? "" : r.getReference() + " ") + r.getName()).replaceAll("[^\\p{L}\\p{N} ._-]", "_");
        return new FileContent(sink.read(r.getCompanyId(), r.getFinalDocumentId()), name + " (signed).pdf", "application/pdf");
    }

    // ------------------------------------------------------------------ writes

    @Override
    public PublicResultResponse submit(String token, PublicSubmitCommand command, String ip, String userAgent) {
        Resolved first = resolve(token, ip);
        if (command == null || !command.consent()) {
            throw new SignDomainException("error.sign.consent", null, "You must agree to sign electronically first");
        }
        List<FieldValue> values = decode(command.values());
        UUID requestId = first.request().getId();
        SubmitOutcome outcome = tx.execute(status -> {
            SignRequest r = requests.findByTokenHash(first.hash()).orElseThrow(PublicSigningApplicationServiceImpl::notAvailable);
            SignerItem s = r.signerByTokenHash(first.hash()).orElseThrow(PublicSigningApplicationServiceImpl::notAvailable);
            Instant now = access.now();
            SubmitResult result = r.submit(s.getId(), values, ip, userAgent, now);
            if (result == SubmitResult.SIGNED) {
                requests.save(r);
                recorder.event(r.getId(), s.getId(), EventType.SIGNED, now, ip, userAgent, "Signed as " + s.getRoleName());
                recorder.audit(r.getCompanyId(), r.getId(), "Signer signed", java.util.Map.of("role", s.getRoleName()));
                r.signerAwaitingLink().ifPresent(next -> finalizer.notifyOwner(r, "Next signer ready: " + r.getName(),
                        next.getName() + " (" + next.getRoleName() + ") can sign now. Open the request to copy their link."));
                if (!r.allSigned()) {
                    finalizer.notifyOwner(r, "Signed by " + s.getName() + ": " + r.getName(),
                            r.signedCount() + " of " + r.getSigners().size() + " signed.");
                }
            }
            return new SubmitOutcome(r.awaitingFinalDocument());
        });
        if (outcome != null && outcome.needsFinalPdf()) {
            finalizer.finalizeRequest(requestId);          // after the signature is committed; the retry job covers failures
        }
        SignRequest after = requests.findAny(requestId).orElseThrow(PublicSigningApplicationServiceImpl::notAvailable);
        boolean done = after.getStatus() == RequestStatus.COMPLETED;
        return new PublicResultResponse(done ? "COMPLETED" : "DONE", done, done);
    }

    private record SubmitOutcome(boolean needsFinalPdf) {
    }

    @Override
    public PublicResultResponse refuse(String token, PublicRefuseCommand command, String ip, String userAgent) {
        Resolved res = resolve(token, ip);
        tx.executeWithoutResult(status -> {
            SignRequest r = requests.findByTokenHash(res.hash()).orElseThrow(PublicSigningApplicationServiceImpl::notAvailable);
            SignerItem s = r.signerByTokenHash(res.hash()).orElseThrow(PublicSigningApplicationServiceImpl::notAvailable);
            Instant now = access.now();
            r.refuse(s.getId(), command == null ? null : command.reason(), ip, userAgent, now);
            requests.save(r);
            recorder.event(r.getId(), s.getId(), EventType.REFUSED, now, ip, userAgent, command.reason().trim());
            recorder.audit(r.getCompanyId(), r.getId(), "Signer refused", java.util.Map.of("role", s.getRoleName()));
            finalizer.notifyOwner(r, "Refused: " + r.getName(), s.getName() + " refused to sign. Reason: " + command.reason().trim());
        });
        return new PublicResultResponse("REFUSED", false, false);
    }

    // ------------------------------------------------------------------ helpers

    /** Finds the request and signer behind a link, or fails with the same neutral 404 for every reason. */
    private Resolved resolve(String token, String ip) {
        Instant now = access.now();
        if (ip != null) {
            limiter.check(ip, now);
        }
        if (!Tokens.wellFormed(token)) {
            limiter.failure(ip, now);
            throw notAvailable();
        }
        String hash = Tokens.hash(token);
        SignRequest r = requests.findByTokenHash(hash).orElse(null);
        SignerItem s = r == null ? null : r.signerByTokenHash(hash).orElse(null);
        boolean usable = s != null && s.getTokenExpiresAt() != null && s.getTokenExpiresAt().isAfter(now)
                && (r.getStatus() == RequestStatus.COMPLETED || (r.isLive() && r.getExpiresAt() != null && r.getExpiresAt().isAfter(now)));
        if (!usable) {
            limiter.failure(ip, now);
            throw notAvailable();
        }
        return new Resolved(r, s, hash);
    }

    private PublicSigningView view(SignRequest r, SignerItem s) {
        int total = r.getSigners().size();
        if (r.getStatus() == RequestStatus.COMPLETED) {
            return new PublicSigningView("COMPLETED", r.getName(), r.getMessage(), s.getName(), s.getRoleName(), r.getPageCount(),
                    r.signedCount(), total, List.of(), r.getFinalDocumentId() != null, s.getChannel().name());
        }
        if (s.getStatus() == SignerStatus.WAITING) {
            return new PublicSigningView("WAITING", r.getName(), null, s.getName(), s.getRoleName(), 0, r.signedCount(), total,
                    List.of(), false, s.getChannel().name());
        }
        if (s.getStatus() == SignerStatus.SIGNED) {
            return new PublicSigningView("DONE", r.getName(), r.getMessage(), s.getName(), s.getRoleName(), r.getPageCount(),
                    r.signedCount(), total, List.of(), false, s.getChannel().name());
        }
        List<PublicFieldResponse> fields = new ArrayList<>();
        for (RequestField f : r.fieldsOf(s.getId())) {
            var g = f.getGeometry();
            fields.add(new PublicFieldResponse(f.getId(), g.page(), g.x(), g.y(), g.width(), g.height(), f.getType().name(),
                    f.isRequired(), f.getLabel(), f.getPlaceholder(), prefill(f, s)));
        }
        return new PublicSigningView("READY", r.getName(), r.getMessage(), s.getName(), s.getRoleName(), r.getPageCount(),
                r.signedCount(), total, fields, false, s.getChannel().name());
    }

    private String prefill(RequestField f, SignerItem s) {
        return switch (f.getAutoFill()) {
            case SIGNER_NAME -> s.getName();
            case SIGNER_EMAIL -> s.getEmail();
            case TODAY -> LocalDate.now(zone).toString();
            case NONE -> null;
        };
    }

    private List<FieldValue> decode(List<PublicValue> in) {
        List<FieldValue> out = new ArrayList<>();
        for (PublicValue v : in == null ? List.<PublicValue>of() : in) {
            byte[] image = null;
            if (v.imagePng() != null && !v.imagePng().isBlank()) {
                String data = v.imagePng();
                int comma = data.indexOf(',');
                if (data.startsWith("data:") && comma > 0) {
                    data = data.substring(comma + 1);
                }
                byte[] raw;
                try {
                    raw = Base64.getMimeDecoder().decode(data);
                } catch (IllegalArgumentException e) {
                    throw new SignDomainException("error.sign.image.invalid", null, "The signature image is not valid");
                }
                if (raw.length > 4 * 1024 * 1024) {
                    throw new SignDomainException("error.sign.image.tooBig", null, "The signature image is too large");
                }
                image = pdf.sanitizeSignatureImage(raw);
            }
            out.add(new FieldValue(v.fieldId(), v.text(), v.bool(), image));
        }
        return out;
    }

    static ResponseStatusException notAvailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not available");
    }
}
