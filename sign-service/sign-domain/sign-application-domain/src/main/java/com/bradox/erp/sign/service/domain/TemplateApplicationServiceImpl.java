package com.bradox.erp.sign.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.domain.core.entity.SignRequestLimits;
import com.bradox.erp.sign.domain.core.entity.Template;
import com.bradox.erp.sign.domain.core.entity.TemplateField;
import com.bradox.erp.sign.domain.core.entity.TemplateRole;
import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.domain.core.rule.FieldGeometry;
import com.bradox.erp.sign.domain.core.valueobject.AutoFill;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;
import com.bradox.erp.sign.service.domain.dto.CreateTemplateCommand;
import com.bradox.erp.sign.service.domain.dto.FieldDto;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.ReplaceDocumentCommand;
import com.bradox.erp.sign.service.domain.dto.RoleDto;
import com.bradox.erp.sign.service.domain.dto.TemplateResponse;
import com.bradox.erp.sign.service.domain.dto.TemplateSummaryResponse;
import com.bradox.erp.sign.service.domain.dto.UpdateTemplateCommand;
import com.bradox.erp.sign.service.domain.ports.input.TemplateApplicationService;
import com.bradox.erp.sign.service.domain.ports.output.PdfPort;
import com.bradox.erp.sign.service.domain.ports.output.SourceDocumentPort;
import com.bradox.erp.sign.service.domain.ports.output.SourceDocumentPort.SourceDoc;
import com.bradox.erp.sign.service.domain.ports.output.repository.TemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class TemplateApplicationServiceImpl implements TemplateApplicationService {

    static final float EDITOR_SCALE = 1.5f;

    private final TemplateRepository templates;
    private final SourceDocumentPort sources;
    private final PdfPort pdf;
    private final PageCache pageCache;
    private final SignAccess access;
    private final SignRecorder recorder;

    TemplateApplicationServiceImpl(TemplateRepository templates, SourceDocumentPort sources, PdfPort pdf, PageCache pageCache,
                                   SignAccess access, SignRecorder recorder) {
        this.templates = templates;
        this.sources = sources;
        this.pdf = pdf;
        this.pageCache = pageCache;
        this.access = access;
        this.recorder = recorder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateSummaryResponse> list(CompanyId companyId, boolean includeArchived) {
        access.require(SignPermissions.TEMPLATE_VIEW);
        return templates.list(companyId, includeArchived && access.can(SignPermissions.TEMPLATE_MANAGE)).stream()
                .map(SignMapper::summary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse get(CompanyId companyId, UUID id) {
        access.require(SignPermissions.TEMPLATE_VIEW);
        Template t = load(companyId, id);
        return SignMapper.template(t, templates.countRequests(companyId, id));
    }

    @Override
    @Transactional
    public TemplateResponse create(CompanyId companyId, CreateTemplateCommand c) {
        access.require(SignPermissions.TEMPLATE_MANAGE);
        SourceDoc doc = sources.readCurrent(companyId, c.documentId());
        PdfPort.PdfInfo info = pdf.inspect(doc.bytes());
        int validity = c.defaultValidityDays() == null ? SignRequestLimits.DEFAULT_VALIDITY_DAYS : c.defaultValidityDays();
        Template t = new Template(UUID.randomUUID(), companyId, c.name().trim(), doc.documentId(), doc.versionId(), doc.sha256(),
                info.pageCount(), false, validity, c.defaultMessage(), c.category(), access.now(), access.actorLabel());
        t.update(c.name(), validity, c.defaultMessage(), c.category(), List.of(), List.of(), false);
        Template saved = templates.save(t);
        recorder.audit(companyId, saved.getId(), "Template created", Map.of("name", saved.getName()));
        return SignMapper.template(saved, 0);
    }

    @Override
    @Transactional
    public TemplateResponse update(CompanyId companyId, UUID id, UpdateTemplateCommand c) {
        access.require(SignPermissions.TEMPLATE_MANAGE);
        Template t = load(companyId, id);
        int validity = c.defaultValidityDays() == null ? t.getDefaultValidityDays() : c.defaultValidityDays();
        t.update(c.name(), validity, c.defaultMessage(), c.category(), roles(c.roles()), fields(c.fields()), c.active());
        Template saved = templates.save(t);
        recorder.audit(companyId, id, "Template updated", Map.of("roles", saved.getRoles().size(), "fields", saved.getFields().size()));
        return SignMapper.template(saved, templates.countRequests(companyId, id));
    }

    @Override
    @Transactional
    public TemplateResponse replaceDocument(CompanyId companyId, UUID id, ReplaceDocumentCommand c) {
        access.require(SignPermissions.TEMPLATE_MANAGE);
        Template t = load(companyId, id);
        SourceDoc doc = sources.readCurrent(companyId, c.documentId());
        PdfPort.PdfInfo info = pdf.inspect(doc.bytes());
        boolean kept = t.replaceDocument(doc.documentId(), doc.versionId(), doc.sha256(), info.pageCount());
        Template saved = templates.save(t);
        recorder.audit(companyId, id, "Template document replaced", Map.of("fieldsKept", kept));
        return SignMapper.template(saved, templates.countRequests(companyId, id));
    }

    @Override
    @Transactional
    public TemplateResponse duplicate(CompanyId companyId, UUID id) {
        access.require(SignPermissions.TEMPLATE_MANAGE);
        Template src = load(companyId, id);
        Template copy = new Template(UUID.randomUUID(), companyId, src.getName() + " (copy)", src.getDocumentId(),
                src.getDocumentVersionId(), src.getDocumentSha256(), src.getPageCount(), false, src.getDefaultValidityDays(),
                src.getDefaultMessage(), src.getCategory(), access.now(), access.actorLabel());
        Map<UUID, UUID> roleIds = new java.util.HashMap<>();
        List<TemplateRole> roles = new ArrayList<>();
        for (TemplateRole r : src.getRoles()) {
            UUID nid = UUID.randomUUID();
            roleIds.put(r.getId(), nid);
            roles.add(new TemplateRole(nid, r.getName(), r.getColor(), r.getSequence(), r.isApproverOnly(), r.getDefaultPartnerId()));
        }
        List<TemplateField> fields = new ArrayList<>();
        for (TemplateField f : src.getFields()) {
            fields.add(new TemplateField(UUID.randomUUID(), roleIds.get(f.getRoleId()), f.getGeometry(), f.getType(), f.isRequired(),
                    f.getLabel(), f.getPlaceholder(), f.getAutoFill()));
        }
        copy.update(copy.getName(), copy.getDefaultValidityDays(), copy.getDefaultMessage(), copy.getCategory(), roles, fields, false);
        Template saved = templates.save(copy);
        recorder.audit(companyId, saved.getId(), "Template duplicated", Map.of("from", id.toString()));
        return SignMapper.template(saved, 0);
    }

    @Override
    @Transactional
    public TemplateResponse archive(CompanyId companyId, UUID id) {
        access.require(SignPermissions.TEMPLATE_MANAGE);
        Template t = load(companyId, id);
        t.archive();
        Template saved = templates.save(t);
        recorder.audit(companyId, id, "Template archived", Map.of());
        return SignMapper.template(saved, templates.countRequests(companyId, id));
    }

    @Override
    @Transactional
    public TemplateResponse restore(CompanyId companyId, UUID id) {
        access.require(SignPermissions.TEMPLATE_MANAGE);
        Template t = load(companyId, id);
        t.validateForActivation();
        t.update(t.getName(), t.getDefaultValidityDays(), t.getDefaultMessage(), t.getCategory(), t.getRoles(), t.getFields(), true);
        Template saved = templates.save(t);
        recorder.audit(companyId, id, "Template restored", Map.of());
        return SignMapper.template(saved, templates.countRequests(companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageImage page(CompanyId companyId, UUID id, int page) {
        access.require(SignPermissions.TEMPLATE_VIEW);
        Template t = load(companyId, id);
        if (page < 1 || page > t.getPageCount()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
        }
        return pageCache.get(t.getDocumentSha256(), page, EDITOR_SCALE, () -> {
            SourceDoc doc = sources.readPinned(companyId, t.getDocumentId(), t.getDocumentVersionId());
            return pdf.render(doc.bytes(), page, EDITOR_SCALE);
        });
    }

    private Template load(CompanyId companyId, UUID id) {
        return templates.find(companyId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
    }

    private static List<TemplateRole> roles(List<RoleDto> in) {
        List<TemplateRole> out = new ArrayList<>();
        if (in == null) {
            return out;
        }
        int seq = 1;
        for (RoleDto r : in) {
            out.add(new TemplateRole(r.id() == null ? UUID.randomUUID() : r.id(), r.name(), r.color(),
                    r.sequence() > 0 ? r.sequence() : seq, r.approverOnly(), r.defaultPartnerId()));
            seq++;
        }
        return out;
    }

    private static List<TemplateField> fields(List<FieldDto> in) {
        List<TemplateField> out = new ArrayList<>();
        if (in == null) {
            return out;
        }
        for (FieldDto f : in) {
            UUID role;
            try {
                role = UUID.fromString(f.roleKey());
            } catch (RuntimeException e) {
                throw new SignDomainException("error.sign.field.role", null, "A field belongs to a role that does not exist");
            }
            out.add(new TemplateField(f.id() == null ? UUID.randomUUID() : f.id(), role,
                    new FieldGeometry(f.page(), f.x(), f.y(), f.width(), f.height()), parseType(f.type()), f.required(), f.label(),
                    f.placeholder(), parseAutoFill(f.autoFill())));
        }
        return out;
    }

    static FieldType parseType(String s) {
        try {
            return FieldType.valueOf(s);
        } catch (RuntimeException e) {
            throw new SignDomainException("error.sign.field.type", null, "Unknown field type");
        }
    }

    static AutoFill parseAutoFill(String s) {
        if (s == null || s.isBlank()) {
            return AutoFill.NONE;
        }
        try {
            return AutoFill.valueOf(s);
        } catch (RuntimeException e) {
            throw new SignDomainException("error.sign.field.autofill", null, "Unknown auto-fill option");
        }
    }
}
