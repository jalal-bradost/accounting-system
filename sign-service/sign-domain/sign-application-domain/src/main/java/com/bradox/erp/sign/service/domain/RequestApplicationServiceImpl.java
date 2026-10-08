package com.bradox.erp.sign.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.sign.domain.core.entity.RequestField;
import com.bradox.erp.sign.domain.core.entity.SignRequest;
import com.bradox.erp.sign.domain.core.entity.SignRequestLimits;
import com.bradox.erp.sign.domain.core.entity.SignerItem;
import com.bradox.erp.sign.domain.core.entity.Template;
import com.bradox.erp.sign.domain.core.entity.TemplateField;
import com.bradox.erp.sign.domain.core.entity.TemplateRole;
import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.domain.core.rule.FieldGeometry;
import com.bradox.erp.sign.domain.core.valueobject.Channel;
import com.bradox.erp.sign.domain.core.valueobject.EventType;
import com.bradox.erp.sign.domain.core.valueobject.RequestStatus;
import com.bradox.erp.sign.domain.core.valueobject.SigningOrder;
import com.bradox.erp.sign.service.domain.dto.CreateRequestCommand;
import com.bradox.erp.sign.service.domain.dto.DashboardResponse;
import com.bradox.erp.sign.service.domain.dto.EventResponse;
import com.bradox.erp.sign.service.domain.dto.ExtendCommand;
import com.bradox.erp.sign.service.domain.dto.FieldDto;
import com.bradox.erp.sign.service.domain.dto.FileContent;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.ReasonCommand;
import com.bradox.erp.sign.service.domain.dto.ReminderTextResponse;
import com.bradox.erp.sign.service.domain.dto.RequestFilter;
import com.bradox.erp.sign.service.domain.dto.RequestResponse;
import com.bradox.erp.sign.service.domain.dto.RequestSummaryResponse;
import com.bradox.erp.sign.service.domain.dto.SendResultResponse;
import com.bradox.erp.sign.service.domain.dto.SignerInput;
import com.bradox.erp.sign.service.domain.dto.SignerLinkResponse;
import com.bradox.erp.sign.service.domain.dto.UpdateRequestCommand;
import com.bradox.erp.sign.service.domain.ports.input.RequestApplicationService;
import com.bradox.erp.sign.service.domain.ports.output.PdfPort;
import com.bradox.erp.sign.service.domain.ports.output.SignedDocumentSinkPort;
import com.bradox.erp.sign.service.domain.ports.output.SignerDirectoryPort;
import com.bradox.erp.sign.service.domain.ports.output.SignerDirectoryPort.Person;
import com.bradox.erp.sign.service.domain.ports.output.SourceDocumentPort;
import com.bradox.erp.sign.service.domain.ports.output.SourceDocumentPort.SourceDoc;
import com.bradox.erp.sign.service.domain.ports.output.repository.EventRepository;
import com.bradox.erp.sign.service.domain.ports.output.repository.RequestRepository;
import com.bradox.erp.sign.service.domain.ports.output.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class RequestApplicationServiceImpl implements RequestApplicationService {

    private static final float VIEW_SCALE = 1.5f;
    private static final List<String> LIVE = List.of("SENT", "IN_PROGRESS");

    private final RequestRepository requests;
    private final TemplateRepository templates;
    private final EventRepository events;
    private final SourceDocumentPort sources;
    private final SignedDocumentSinkPort sink;
    private final SignerDirectoryPort directory;
    private final PdfPort pdf;
    private final PageCache pageCache;
    private final SignAccess access;
    private final SignRecorder recorder;
    private final String baseUrl;

    RequestApplicationServiceImpl(RequestRepository requests, TemplateRepository templates, EventRepository events,
                                  SourceDocumentPort sources, SignedDocumentSinkPort sink, SignerDirectoryPort directory, PdfPort pdf,
                                  PageCache pageCache, SignAccess access, SignRecorder recorder,
                                  @Value("${app.sign.base-url:http://localhost:3000}") String baseUrl) {
        this.requests = requests;
        this.templates = templates;
        this.events = events;
        this.sources = sources;
        this.sink = sink;
        this.directory = directory;
        this.pdf = pdf;
        this.pageCache = pageCache;
        this.access = access;
        this.recorder = recorder;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    // ------------------------------------------------------------------ reads

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RequestSummaryResponse> list(CompanyId companyId, RequestFilter filter) {
        requireView();
        UUID me = access.can(SignPermissions.REQUEST_VIEW_ALL) && !filter.mineOnly() ? null : access.requireUserId();
        Instant now = access.now();
        PageResponse<SignRequest> page = requests.search(companyId, filter, me);
        return new PageResponse<>(page.getContent().stream().map(r -> SignMapper.summary(r, now)).toList(), page.getPage(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public RequestResponse get(CompanyId companyId, UUID id) {
        return response(visible(companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestSummaryResponse> byRecord(CompanyId companyId, String recordModel, UUID recordId) {
        requireView();
        Instant now = access.now();
        return requests.findByRecord(companyId, recordModel, recordId).stream().filter(this::mayView)
                .map(r -> SignMapper.summary(r, now)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> events(CompanyId companyId, UUID id) {
        SignRequest r = visible(companyId, id);
        Map<UUID, String> names = new HashMap<>();
        r.getSigners().forEach(s -> names.put(s.getId(), s.getName()));
        boolean audit = access.can(SignPermissions.AUDIT_VIEW);
        return events.list(id).stream().map(e -> SignMapper.event(e, names, audit)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageImage page(CompanyId companyId, UUID id, int page) {
        SignRequest r = visible(companyId, id);
        if (page < 1 || page > r.getPageCount()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
        }
        return pageCache.get(r.getSourceSha256(), page, VIEW_SCALE, () -> pdf.render(
                sources.readPinned(companyId, r.getSourceDocumentId(), r.getSourceVersionId()).bytes(), page, VIEW_SCALE));
    }

    @Override
    @Transactional(readOnly = true)
    public FileContent finalDocument(CompanyId companyId, UUID id) {
        SignRequest r = visible(companyId, id);
        if (r.getFinalDocumentId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not finished yet");
        }
        return new FileContent(sink.read(companyId, r.getFinalDocumentId()), fileName(r), "application/pdf");
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(CompanyId companyId) {
        requireView();
        UUID me = access.requireUserId();
        UUID scope = access.can(SignPermissions.REQUEST_VIEW_ALL) ? null : me;
        Instant now = access.now();
        Instant monthStart = now.atZone(ZoneOffset.UTC).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        long waitingForOthers = requests.countByStatus(companyId, scope, LIVE);
        long waitingForMe = requests.countWaitingForUser(companyId, me);
        long done = requests.countCompletedSince(companyId, scope, monthStart);
        long expiring = requests.countExpiringBefore(companyId, scope, now.plus(7, ChronoUnit.DAYS));
        RequestFilter live = new RequestFilter(LIVE, null, null, null, null, null, null, null, scope != null, false, 0, 200);
        List<SignRequest> open = requests.search(companyId, live, scope).getContent();
        long attention = open.stream().filter(SignRequest::isNeedsAttention).count();
        long due = open.stream().filter(r -> r.reminderDue(now)).count();
        return new DashboardResponse(waitingForMe, waitingForOthers, done, expiring, attention, due);
    }

    // ------------------------------------------------------------------ draft

    @Override
    @Transactional
    public RequestResponse create(CompanyId companyId, CreateRequestCommand c) {
        access.require(SignPermissions.REQUEST_CREATE);
        Built b = build(companyId, c.templateId(), c.documentId(), c.signers(), c.fields(), null);
        SignRequest r = new SignRequest(UUID.randomUUID(), companyId, c.name().trim(), parseOrder(c.signingOrder()), c.message(),
                c.templateId(), b.docId, b.versionId, b.sha256, b.pageCount, c.recordModel(), c.recordId(), c.reminderEveryDays(),
                access.now(), access.actorLabel(), access.requireUserId());
        r.assignReference(requests.nextReference(companyId, access.now().atZone(ZoneOffset.UTC).getYear()));
        r.editDraft(c.name(), parseOrder(c.signingOrder()), c.message(), c.reminderEveryDays(), b.signers, b.fields);
        SignRequest saved = requests.save(r);
        recorder.event(saved.getId(), null, EventType.CREATED, access.now(), null, null, "Created by " + access.actorLabel());
        recorder.audit(companyId, saved.getId(), "Signature request created", Map.of("reference", saved.getReference()));
        return response(saved);
    }

    @Override
    @Transactional
    public RequestResponse update(CompanyId companyId, UUID id, UpdateRequestCommand c) {
        SignRequest r = owned(companyId, id);
        Built b = build(companyId, r.getTemplateId(), r.getTemplateId() == null ? r.getSourceDocumentId() : null, c.signers(),
                c.fields(), r);
        r.editDraft(c.name(), parseOrder(c.signingOrder()), c.message(), c.reminderEveryDays(), b.signers, b.fields);
        SignRequest saved = requests.save(r);
        recorder.audit(companyId, id, "Draft updated", Map.of());
        return response(saved);
    }

    @Override
    @Transactional
    public void delete(CompanyId companyId, UUID id) {
        SignRequest r = owned(companyId, id);
        if (r.getStatus() != RequestStatus.DRAFT) {
            throw new SignDomainException("error.sign.request.state", null, "Only a draft can be deleted");
        }
        requests.delete(companyId, id);
        recorder.audit(companyId, id, "Draft deleted", Map.of());
    }

    // ------------------------------------------------------------------ sending

    @Override
    @Transactional
    public SendResultResponse send(CompanyId companyId, UUID id, Integer validityDays) {
        SignRequest r = owned(companyId, id);
        if (r.getTemplateId() == null) {
            SourceDoc doc = sources.readCurrent(companyId, r.getSourceDocumentId());
            if (!doc.sha256().equals(r.getSourceSha256())) {
                r.repin(doc.versionId(), doc.sha256(), pdf.inspect(doc.bytes()).pageCount());
            }
        } else {
            SourceDoc doc = sources.readPinned(companyId, r.getSourceDocumentId(), r.getSourceVersionId());
            if (!doc.sha256().equals(r.getSourceSha256())) {
                throw new SignDomainException("error.sign.source.changed", null, "The document changed since this draft was made");
            }
        }
        int days = validityDays != null ? validityDays
                : r.getTemplateId() == null ? SignRequestLimits.DEFAULT_VALIDITY_DAYS
                : templates.find(companyId, r.getTemplateId()).map(Template::getDefaultValidityDays)
                        .orElse(SignRequestLimits.DEFAULT_VALIDITY_DAYS);
        Instant now = access.now();
        Map<UUID, String> tokens = r.send(now, now.plus(days, ChronoUnit.DAYS));
        SignRequest saved = requests.save(r);
        recorder.event(id, null, EventType.SENT, now, null, null, saved.getSigners().size() + " signer(s), order "
                + saved.getOrder().name().toLowerCase());
        recorder.audit(companyId, id, "Signature request sent", Map.of("reference", saved.getReference()));
        List<SignerLinkResponse> links = new ArrayList<>();
        for (SignerItem s : saved.ordered()) {
            String token = tokens.get(s.getId());
            if (token != null) {
                links.add(new SignerLinkResponse(s.getId(), s.getName(), s.getRoleName(), url(token)));
            }
        }
        return new SendResultResponse(response(saved), links);
    }

    @Override
    @Transactional
    public SignerLinkResponse newLink(CompanyId companyId, UUID id, UUID signerId) {
        SignRequest r = owned(companyId, id, false);
        String token = r.issueLink(signerId);
        requests.save(r);
        recorder.event(id, signerId, EventType.LINK_REGENERATED, access.now(), null, null, "New link generated by " + access.actorLabel());
        SignerItem s = r.signer(signerId);
        return new SignerLinkResponse(signerId, s.getName(), s.getRoleName(), url(token));
    }

    @Override
    @Transactional
    public RequestResponse replaceSigner(CompanyId companyId, UUID id, UUID signerId, SignerInput in) {
        SignRequest r = owned(companyId, id, false);
        Person p = resolve(companyId, in);
        r.replaceSigner(signerId, in.partnerId(), in.userId(), p.name(), p.email(), p.phone());
        SignRequest saved = requests.save(r);
        recorder.event(id, signerId, EventType.SIGNER_REPLACED, access.now(), null, null, "Replaced with " + p.name());
        return response(saved);
    }

    // ------------------------------------------------------------------ closing and nudging

    @Override
    @Transactional
    public RequestResponse cancel(CompanyId companyId, UUID id, ReasonCommand c) {
        access.require(SignPermissions.REQUEST_CANCEL);
        SignRequest r = visible(companyId, id);
        if (!access.can(SignPermissions.REQUEST_VIEW_ALL) && !isOwner(r)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found");
        }
        r.cancel(c.reason(), access.now());
        SignRequest saved = requests.save(r);
        recorder.event(id, null, EventType.CANCELED, access.now(), null, null, c.reason());
        recorder.audit(companyId, id, "Signature request canceled", Map.of("reason", c.reason()));
        return response(saved);
    }

    @Override
    @Transactional
    public RequestResponse extend(CompanyId companyId, UUID id, ExtendCommand c) {
        SignRequest r = owned(companyId, id, false);
        r.extend(c.expiresAt(), access.now());
        SignRequest saved = requests.save(r);
        recorder.event(id, null, EventType.EXTENDED, access.now(), null, null, "Now expires " + c.expiresAt());
        return response(saved);
    }

    @Override
    @Transactional
    public ReminderTextResponse remind(CompanyId companyId, UUID id, UUID signerId) {
        SignRequest r = owned(companyId, id, false);
        SignerItem target = signerId != null ? r.signer(signerId)
                : r.ordered().stream().filter(SignerItem::isActive).findFirst()
                        .orElseThrow(() -> new SignDomainException("error.sign.remind.nobody", null, "Nobody is waiting to sign"));
        String token = r.issueLink(target.getId());
        r.recordReminder(access.now());
        requests.save(r);
        recorder.event(id, target.getId(), EventType.REMINDED, access.now(), null, null, "Reminder prepared for " + target.getName());
        String url = url(token);
        String until = r.getExpiresAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
        return new ReminderTextResponse(url,
                "Hello " + target.getName() + ", please sign \"" + r.getName() + "\" using this personal link: " + url
                        + " (valid until " + until + ").",
                "مرحباً " + target.getName() + "، يرجى توقيع \"" + r.getName() + "\" عبر هذا الرابط الشخصي: " + url
                        + " (صالح حتى " + until + ").",
                "سڵاو " + target.getName() + "، تکایە \"" + r.getName() + "\" واژۆ بکە بەم بەستەرە تایبەتییە: " + url
                        + " (کاردەکات تا " + until + ").");
    }

    // ------------------------------------------------------------------ helpers

    private record Built(UUID docId, UUID versionId, String sha256, int pageCount, List<SignerItem> signers, List<RequestField> fields) {
    }

    /** Turns the form into signers and fields, from a template or one-off. Existing {@code draft} keeps its pin. */
    private Built build(CompanyId companyId, UUID templateId, UUID documentId, List<SignerInput> inputs, List<FieldDto> fieldDtos,
                        SignRequest draft) {
        List<SignerInput> in = inputs == null ? List.of() : inputs;
        List<SignerItem> signers = new ArrayList<>();
        List<RequestField> fields = new ArrayList<>();
        UUID docId;
        UUID versionId;
        String sha;
        int pages;
        if (templateId != null) {
            Template t = templates.find(companyId, templateId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
            if (!t.isActive()) {
                throw new SignDomainException("error.sign.template.inactive", null, "This template is not active");
            }
            docId = t.getDocumentId();
            versionId = draft != null ? draft.getSourceVersionId() : t.getDocumentVersionId();
            sha = draft != null ? draft.getSourceSha256() : t.getDocumentSha256();
            pages = t.getPageCount();
            Map<UUID, UUID> signerByRole = new HashMap<>();
            for (TemplateRole role : t.getRoles().stream().sorted(java.util.Comparator.comparingInt(TemplateRole::getSequence)).toList()) {
                SignerInput si = in.stream().filter(x -> role.getId().toString().equals(x.roleKey())).findFirst().orElse(null);
                Person p = si == null ? defaultPerson(companyId, role) : resolve(companyId, si);
                SignerItem item = new SignerItem(UUID.randomUUID(), role.getName(), signers.size() + 1, role.isApproverOnly(),
                        si == null ? role.getDefaultPartnerId() : si.partnerId(), si == null ? null : si.userId(), p.name(), p.email(),
                        p.phone(), si == null ? Channel.LINK : channel(si.channel()));
                signers.add(item);
                signerByRole.put(role.getId(), item.getId());
            }
            for (TemplateField f : t.getFields()) {
                UUID owner = signerByRole.get(f.getRoleId());
                if (owner != null) {
                    fields.add(new RequestField(UUID.randomUUID(), owner, f.getGeometry(), f.getType(), f.isRequired(), f.getLabel(),
                            f.getPlaceholder(), f.getAutoFill()));
                }
            }
        } else {
            if (documentId == null) {
                throw new SignDomainException("error.sign.request.noDocument", null, "Choose a template or a document");
            }
            if (draft != null) {
                docId = documentId;
                versionId = draft.getSourceVersionId();
                sha = draft.getSourceSha256();
                pages = draft.getPageCount();
            } else {
                SourceDoc doc = sources.readCurrent(companyId, documentId);
                pages = pdf.inspect(doc.bytes()).pageCount();
                docId = doc.documentId();
                versionId = doc.versionId();
                sha = doc.sha256();
            }
            Map<String, UUID> signerByKey = new HashMap<>();
            int n = 0;
            for (SignerInput si : in) {
                n++;
                String key = si.roleKey() == null || si.roleKey().isBlank() ? "signer-" + n : si.roleKey();
                Person p = resolve(companyId, si);
                SignerItem item = new SignerItem(UUID.randomUUID(), si.roleName() == null || si.roleName().isBlank() ? "Signer " + n
                        : si.roleName(), n, si.approverOnly(), si.partnerId(), si.userId(), p.name(), p.email(), p.phone(),
                        channel(si.channel()));
                signers.add(item);
                signerByKey.put(key, item.getId());
            }
            for (FieldDto f : fieldDtos == null ? List.<FieldDto>of() : fieldDtos) {
                UUID owner = signerByKey.get(f.roleKey());
                if (owner == null) {
                    throw new SignDomainException("error.sign.field.role", null, "A field belongs to a signer that does not exist");
                }
                fields.add(new RequestField(UUID.randomUUID(), owner, new FieldGeometry(f.page(), f.x(), f.y(), f.width(), f.height()),
                        TemplateApplicationServiceImpl.parseType(f.type()), f.required(), f.label(), f.placeholder(),
                        TemplateApplicationServiceImpl.parseAutoFill(f.autoFill())));
            }
        }
        return new Built(docId, versionId, sha, pages, signers, fields);
    }

    private Person defaultPerson(CompanyId companyId, TemplateRole role) {
        if (role.getDefaultPartnerId() != null) {
            return directory.partner(companyId, role.getDefaultPartnerId()).orElse(new Person(null, "", null, null));
        }
        return new Person(null, "", null, null);
    }

    /** Fills name, email and phone from the linked contact or user; typed values win. */
    private Person resolve(CompanyId companyId, SignerInput in) {
        Person linked = null;
        if (in.partnerId() != null) {
            linked = directory.partner(companyId, in.partnerId()).orElse(null);
        } else if (in.userId() != null) {
            linked = directory.user(companyId, in.userId()).orElse(null);
        }
        String name = notBlank(in.name()) ? in.name().trim() : linked != null ? linked.name() : "";
        String email = notBlank(in.email()) ? in.email().trim() : linked != null ? linked.email() : null;
        String phone = notBlank(in.phone()) ? in.phone().trim() : linked != null ? linked.phone() : null;
        return new Person(null, name, email, phone);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static Channel channel(String s) {
        return "IN_PERSON".equals(s) ? Channel.IN_PERSON : Channel.LINK;
    }

    private static SigningOrder parseOrder(String s) {
        return "PARALLEL".equals(s) ? SigningOrder.PARALLEL : SigningOrder.SEQUENTIAL;
    }

    private String url(String token) {
        return baseUrl + "/sign/" + token;
    }

    private String fileName(SignRequest r) {
        String base = (r.getReference() == null ? r.getName() : r.getReference() + " " + r.getName()).replaceAll("[^\\p{L}\\p{N} ._-]", "_");
        return base + " (signed).pdf";
    }

    private void requireView() {
        if (!access.can(SignPermissions.REQUEST_VIEW) && !access.can(SignPermissions.REQUEST_VIEW_ALL)) {
            access.require(SignPermissions.REQUEST_VIEW);
        }
    }

    private boolean isOwner(SignRequest r) {
        UUID me = access.userId().orElse(null);
        return me != null && me.equals(r.getCreatedByUserId());
    }

    private boolean mayView(SignRequest r) {
        if (access.can(SignPermissions.REQUEST_VIEW_ALL)) {
            return true;
        }
        UUID me = access.userId().orElse(null);
        return me != null && (me.equals(r.getCreatedByUserId()) || r.getSigners().stream().anyMatch(s -> me.equals(s.getUserId())));
    }

    private SignRequest visible(CompanyId companyId, UUID id) {
        requireView();
        SignRequest r = requests.find(companyId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!mayView(r)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found");
        }
        return r;
    }

    private SignRequest owned(CompanyId companyId, UUID id) {
        return owned(companyId, id, true);
    }

    /** Requires the create permission and that the request is the caller's (or the caller sees everyone's). */
    private SignRequest owned(CompanyId companyId, UUID id, boolean draftOnly) {
        access.require(SignPermissions.REQUEST_CREATE);
        SignRequest r = requests.find(companyId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!isOwner(r) && !access.can(SignPermissions.REQUEST_VIEW_ALL)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found");
        }
        if (draftOnly && r.getStatus() != RequestStatus.DRAFT && r.getStatus() != RequestStatus.SENT) {
            throw new SignDomainException("error.sign.request.state", null, "Only a draft can be changed");
        }
        return r;
    }

    private RequestResponse response(SignRequest r) {
        boolean manage = isOwner(r) || access.can(SignPermissions.REQUEST_VIEW_ALL);
        boolean canEdit = r.getStatus() == RequestStatus.DRAFT && manage && access.can(SignPermissions.REQUEST_CREATE);
        boolean canCancel = r.isLive() && manage && access.can(SignPermissions.REQUEST_CANCEL);
        return SignMapper.request(r, access.now(), access.can(SignPermissions.AUDIT_VIEW), canEdit, canCancel);
    }
}
