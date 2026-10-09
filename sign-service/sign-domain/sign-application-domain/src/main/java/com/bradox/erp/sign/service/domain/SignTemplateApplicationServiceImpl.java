package com.bradox.erp.sign.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.domain.core.model.SignLimits;
import com.bradox.erp.sign.domain.core.model.SignTemplate;
import com.bradox.erp.sign.domain.core.model.SignedDocument;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;
import com.bradox.erp.sign.service.domain.dto.FieldDto;
import com.bradox.erp.sign.service.domain.dto.FileContent;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.SignCommand;
import com.bradox.erp.sign.service.domain.dto.SignedDocumentResponse;
import com.bradox.erp.sign.service.domain.dto.TemplateCommand;
import com.bradox.erp.sign.service.domain.dto.TemplateResponse;
import com.bradox.erp.sign.service.domain.ports.input.SignTemplateApplicationService;
import com.bradox.erp.sign.service.domain.ports.output.PdfPort;
import com.bradox.erp.sign.service.domain.ports.output.PdfPort.Placement;
import com.bradox.erp.sign.service.domain.ports.output.SignFilePort;
import com.bradox.erp.sign.service.domain.ports.output.SignTemplateRepository;
import com.bradox.erp.sign.service.domain.ports.output.SignedDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class SignTemplateApplicationServiceImpl implements SignTemplateApplicationService {

    static final float PAGE_SCALE = 1.5f;

    private final SignTemplateRepository templates;
    private final SignedDocumentRepository signed;
    private final SignFilePort files;
    private final PdfPort pdf;
    private final PageCache pageCache;
    private final SignAccess access;
    private final ZoneId zone;

    SignTemplateApplicationServiceImpl(SignTemplateRepository templates, SignedDocumentRepository signed, SignFilePort files,
                                       PdfPort pdf, PageCache pageCache, SignAccess access,
                                       @Value("${app.sign.time-zone:Asia/Baghdad}") String zone) {
        this.templates = templates;
        this.signed = signed;
        this.files = files;
        this.pdf = pdf;
        this.pageCache = pageCache;
        this.access = access;
        this.zone = ZoneId.of(zone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> list(CompanyId companyId) {
        access.require(SignPermissions.TEMPLATE_VIEW);
        Map<UUID, Long> counts = signed.countByTemplate(companyId);
        return templates.findAll(companyId).stream().map(t -> toResponse(t, counts.getOrDefault(t.id(), 0L))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse get(CompanyId companyId, UUID id) {
        access.require(SignPermissions.TEMPLATE_VIEW);
        SignTemplate t = load(companyId, id);
        return toResponse(t, signed.countByTemplate(companyId).getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public TemplateResponse create(CompanyId companyId, String name, String fileName, byte[] bytes) {
        access.require(SignPermissions.TEMPLATE_MANAGE);
        PdfPort.PdfInfo info = pdf.inspect(bytes);
        String safeFile = fileName == null || fileName.isBlank() ? "template.pdf" : fileName;
        String templateName = name == null || name.isBlank() ? safeFile.replaceAll("(?i)\\.pdf$", "") : name;
        UUID id = UUID.randomUUID();
        UUID documentId = files.storeTemplate(companyId, id, safeFile, bytes);
        SignTemplate t = new SignTemplate(id, companyId.getId(), templateName, documentId, sha256(bytes), info.pageCount(),
                List.of(), access.now());
        return toResponse(templates.save(t), 0);
    }

    @Override
    @Transactional
    public TemplateResponse update(CompanyId companyId, UUID id, TemplateCommand c) {
        access.require(SignPermissions.TEMPLATE_MANAGE);
        SignTemplate t = load(companyId, id);
        List<SignTemplate.Field> fields = new ArrayList<>();
        for (FieldDto f : c.fields() == null ? List.<FieldDto>of() : c.fields()) {
            fields.add(new SignTemplate.Field(f.id() == null ? UUID.randomUUID() : f.id(), f.page(), f.x(), f.y(), f.width(),
                    f.height(), parseType(f.type())));
        }
        SignTemplate saved = templates.save(t.edit(c.name(), fields));
        return toResponse(saved, signed.countByTemplate(companyId).getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public void delete(CompanyId companyId, UUID id) {
        access.require(SignPermissions.TEMPLATE_MANAGE);
        SignTemplate t = load(companyId, id);
        signed.deleteByTemplate(companyId, id);
        templates.delete(t);
    }

    @Override
    @Transactional(readOnly = true)
    public PageImage page(CompanyId companyId, UUID id, int page) {
        access.require(SignPermissions.TEMPLATE_VIEW);
        SignTemplate t = load(companyId, id);
        if (page < 1 || page > t.pageCount()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
        }
        return pageCache.get(t.documentSha256(), page, PAGE_SCALE,
                () -> pdf.render(files.read(companyId, t.documentId()), page, PAGE_SCALE));
    }

    @Override
    @Transactional
    public SignedDocumentResponse sign(CompanyId companyId, UUID id, SignCommand c) {
        access.require(SignPermissions.SIGN);
        SignTemplate t = load(companyId, id);
        t.checkCanSign();
        String signerName = c.signerName().trim();
        Map<UUID, SignCommand.Value> values = new HashMap<>();
        if (c.values() != null) {
            c.values().stream().filter(v -> v.fieldId() != null).forEach(v -> values.put(v.fieldId(), v));
        }
        var now = access.now();
        String today = DateTimeFormatter.ISO_LOCAL_DATE.format(now.atZone(zone));
        List<Placement> placements = new ArrayList<>();
        for (SignTemplate.Field f : t.fields()) {
            SignCommand.Value v = values.get(f.id());
            switch (f.type()) {
                case SIGNATURE -> {
                    if (v == null || v.imagePng() == null || v.imagePng().isBlank()) {
                        throw new SignDomainException("error.sign.signature.missing", null, "Draw the signature in every signature box");
                    }
                    placements.add(placement(f, null, pdf.sanitizeSignatureImage(decodePng(v.imagePng()))));
                }
                case NAME -> placements.add(placement(f, signerName, null));
                case DATE -> placements.add(placement(f, today, null));
                case TEXT -> {
                    String text = v == null || v.text() == null ? "" : v.text().trim();
                    if (text.length() > SignLimits.MAX_TEXT_LENGTH) {
                        throw new SignDomainException("error.sign.text.tooLong", new Object[]{SignLimits.MAX_TEXT_LENGTH},
                                "Text must be at most " + SignLimits.MAX_TEXT_LENGTH + " characters");
                    }
                    if (!text.isEmpty()) {
                        placements.add(placement(f, text, null));
                    }
                }
            }
        }
        byte[] result = pdf.applyValues(files.read(companyId, t.documentId()), placements);
        UUID signedId = UUID.randomUUID();
        String fileName = (t.name() + " - " + signerName + " - " + today).replaceAll("[^\\p{L}\\p{N} ._-]", "_") + " (signed).pdf";
        UUID documentId = files.storeSigned(companyId, signedId, fileName, result);
        SignedDocument doc = signed.save(new SignedDocument(signedId, companyId.getId(), t.id(), t.name(), signerName, now,
                access.actorLabel(), documentId, fileName));
        return toResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignedDocumentResponse> signedDocuments(CompanyId companyId, UUID templateId) {
        access.require(SignPermissions.TEMPLATE_VIEW);
        load(companyId, templateId);
        return signed.findByTemplate(companyId, templateId).stream().map(SignTemplateApplicationServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FileContent download(CompanyId companyId, UUID signedDocumentId) {
        access.require(SignPermissions.TEMPLATE_VIEW);
        SignedDocument d = signed.find(companyId, signedDocumentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signed document not found"));
        return new FileContent(files.read(companyId, d.documentId()), d.fileName(), "application/pdf");
    }

    private SignTemplate load(CompanyId companyId, UUID id) {
        return templates.find(companyId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
    }

    private static Placement placement(SignTemplate.Field f, String text, byte[] image) {
        return new Placement(f.page(), f.x(), f.y(), f.width(), f.height(), f.type(), text, image);
    }

    private static FieldType parseType(String s) {
        try {
            return FieldType.valueOf(s);
        } catch (RuntimeException e) {
            throw new SignDomainException("error.sign.field.type", null, "Unknown field type");
        }
    }

    /** Accepts plain base64 or a {@code data:image/png;base64,} URL as the browser's canvas produces it. */
    private static byte[] decodePng(String value) {
        String b64 = value.startsWith("data:") ? value.substring(value.indexOf(',') + 1) : value;
        try {
            return Base64.getDecoder().decode(b64.trim());
        } catch (IllegalArgumentException e) {
            throw new SignDomainException("error.sign.image.invalid", null, "The signature image is not valid");
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static TemplateResponse toResponse(SignTemplate t, long signedCount) {
        List<FieldDto> fields = t.fields().stream()
                .map(f -> new FieldDto(f.id(), f.page(), f.x(), f.y(), f.width(), f.height(), f.type().name())).toList();
        return new TemplateResponse(t.id(), t.name(), t.documentId(), t.pageCount(), fields, signedCount, t.createdAt());
    }

    private static SignedDocumentResponse toResponse(SignedDocument d) {
        return new SignedDocumentResponse(d.id(), d.templateId(), d.templateName(), d.signerName(), d.signedAt(), d.signedBy(),
                d.documentId(), d.fileName());
    }
}
