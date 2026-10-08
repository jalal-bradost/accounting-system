package com.bradox.erp.sign.dataaccess.mapper;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.dataaccess.entity.EventEntity;
import com.bradox.erp.sign.dataaccess.entity.RequestEntity;
import com.bradox.erp.sign.dataaccess.entity.RequestFieldEntity;
import com.bradox.erp.sign.dataaccess.entity.SignerEntity;
import com.bradox.erp.sign.dataaccess.entity.TemplateEntity;
import com.bradox.erp.sign.dataaccess.entity.TemplateFieldEntity;
import com.bradox.erp.sign.dataaccess.entity.TemplateRoleEntity;
import com.bradox.erp.sign.domain.core.entity.RequestField;
import com.bradox.erp.sign.domain.core.entity.SignEvent;
import com.bradox.erp.sign.domain.core.entity.SignRequest;
import com.bradox.erp.sign.domain.core.entity.SignerItem;
import com.bradox.erp.sign.domain.core.entity.Template;
import com.bradox.erp.sign.domain.core.entity.TemplateField;
import com.bradox.erp.sign.domain.core.entity.TemplateRole;
import com.bradox.erp.sign.domain.core.rule.FieldGeometry;
import com.bradox.erp.sign.domain.core.valueobject.AutoFill;
import com.bradox.erp.sign.domain.core.valueobject.Channel;
import com.bradox.erp.sign.domain.core.valueobject.EventType;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;
import com.bradox.erp.sign.domain.core.valueobject.RequestStatus;
import com.bradox.erp.sign.domain.core.valueobject.SignerStatus;
import com.bradox.erp.sign.domain.core.valueobject.SigningOrder;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class SignDataMapper {

    // ------------------------------------------------------------------ templates

    public Template toDomain(TemplateEntity e, List<TemplateRoleEntity> roles, List<TemplateFieldEntity> fields) {
        Template t = new Template(e.getId(), new CompanyId(e.getCompanyId()), e.getName(), e.getDocumentId(), e.getDocumentVersionId(),
                e.getDocumentSha256(), e.getPageCount(), e.isActive(), e.getDefaultValidityDays(), e.getDefaultMessage(), e.getCategory(),
                e.getCreatedAt(), e.getCreatedBy());
        // Load the stored layout as is: it was validated when saved, and archived templates may legitimately be incomplete.
        t.restoreLayout(roles.stream().map(r -> new TemplateRole(r.getId(), r.getName(), r.getColor(), r.getSequence(), r.isApproverOnly(),
                r.getDefaultPartnerId())).sorted(java.util.Comparator.comparingInt(TemplateRole::getSequence)).toList(),
                fields.stream().map(f -> new TemplateField(f.getId(), f.getRoleId(),
                        new FieldGeometry(f.getPage(), f.getX(), f.getY(), f.getWidth(), f.getHeight()),
                        FieldType.valueOf(f.getFieldType()), f.isRequired(), f.getLabel(), f.getPlaceholder(),
                        AutoFill.valueOf(f.getAutoFill()))).toList());
        return t;
    }

    public TemplateEntity toEntity(Template t) {
        TemplateEntity e = new TemplateEntity();
        e.setId(t.getId());
        e.setCompanyId(t.getCompanyId().getId());
        e.setName(t.getName());
        e.setCategory(t.getCategory());
        e.setDocumentId(t.getDocumentId());
        e.setDocumentVersionId(t.getDocumentVersionId());
        e.setDocumentSha256(t.getDocumentSha256());
        e.setPageCount(t.getPageCount());
        e.setActive(t.isActive());
        e.setDefaultValidityDays(t.getDefaultValidityDays());
        e.setDefaultMessage(t.getDefaultMessage());
        e.setCreatedAt(t.getCreatedAt());
        e.setCreatedBy(t.getCreatedBy());
        return e;
    }

    public TemplateRoleEntity toEntity(UUID templateId, TemplateRole r) {
        TemplateRoleEntity e = new TemplateRoleEntity();
        e.setId(r.getId());
        e.setTemplateId(templateId);
        e.setName(r.getName());
        e.setColor(r.getColor());
        e.setSequence(r.getSequence());
        e.setApproverOnly(r.isApproverOnly());
        e.setDefaultPartnerId(r.getDefaultPartnerId());
        return e;
    }

    public TemplateFieldEntity toEntity(UUID templateId, TemplateField f) {
        TemplateFieldEntity e = new TemplateFieldEntity();
        e.setId(f.getId());
        e.setTemplateId(templateId);
        e.setRoleId(f.getRoleId());
        var g = f.getGeometry();
        e.setPage(g.page());
        e.setX(g.x());
        e.setY(g.y());
        e.setWidth(g.width());
        e.setHeight(g.height());
        e.setFieldType(f.getType().name());
        e.setRequired(f.isRequired());
        e.setLabel(f.getLabel());
        e.setPlaceholder(f.getPlaceholder());
        e.setAutoFill(f.getAutoFill().name());
        return e;
    }

    // ------------------------------------------------------------------ requests

    public SignRequest toDomain(RequestEntity e, List<SignerEntity> signers, List<RequestFieldEntity> fields) {
        SignRequest r = new SignRequest(e.getId(), new CompanyId(e.getCompanyId()), e.getName(), SigningOrder.valueOf(e.getSigningOrder()),
                e.getMessage(), e.getTemplateId(), e.getSourceDocumentId(), e.getSourceVersionId(), e.getSourceSha256(), e.getPageCount(),
                e.getRecordModel(), e.getRecordId(), e.getReminderEveryDays(), e.getCreatedAt(), e.getCreatedBy(), e.getCreatedByUserId());
        List<SignerItem> items = signers.stream().sorted(java.util.Comparator.comparingInt(SignerEntity::getSequence)).map(s -> {
            SignerItem i = new SignerItem(s.getId(), s.getRoleName(), s.getSequence(), s.isApproverOnly(), s.getPartnerId(), s.getUserId(),
                    s.getName(), s.getEmail(), s.getPhone(), Channel.valueOf(s.getChannel()));
            i.restoreState(s.getTokenHash(), s.getTokenExpiresAt(), SignerStatus.valueOf(s.getStatus()), s.getViewedAt(), s.getSignedAt(),
                    s.getRefusedAt(), s.getRefuseReason(), s.getSignedIp(), s.getSignedUserAgent(), s.getSubmissionDigest(),
                    s.isLinkIssued(), s.getOperatorUserId(), s.isIdChecked(), s.getIdNote());
            return i;
        }).toList();
        List<RequestField> fs = fields.stream().map(f -> {
            RequestField rf = new RequestField(f.getId(), f.getSignerItemId(),
                    new FieldGeometry(f.getPage(), f.getX(), f.getY(), f.getWidth(), f.getHeight()), FieldType.valueOf(f.getFieldType()),
                    f.isRequired(), f.getLabel(), f.getPlaceholder(), AutoFill.valueOf(f.getAutoFill()));
            rf.restoreValue(f.getValueText(), f.getValueBool(), f.getValueImage() == null ? null : Base64.getDecoder().decode(f.getValueImage()));
            return rf;
        }).toList();
        r.restoreState(e.getReference(), RequestStatus.valueOf(e.getStatus()), e.getExpiresAt(), e.getReminderPrompts(), e.getLastReminderAt(),
                e.getFinalDocumentId(), e.getFinalSha256(), e.getSentAt(), e.getCompletedAt(), e.getCanceledAt(), e.getCancelReason(),
                e.isNeedsAttention(), e.getAttentionReason(), e.getBuildAttempts(), items, fs);
        return r;
    }

    public RequestEntity toEntity(SignRequest r) {
        RequestEntity e = new RequestEntity();
        e.setId(r.getId());
        e.setCompanyId(r.getCompanyId().getId());
        e.setReference(r.getReference());
        e.setTemplateId(r.getTemplateId());
        e.setSourceDocumentId(r.getSourceDocumentId());
        e.setSourceVersionId(r.getSourceVersionId());
        e.setSourceSha256(r.getSourceSha256());
        e.setPageCount(r.getPageCount());
        e.setName(r.getName());
        e.setStatus(r.getStatus().name());
        e.setSigningOrder(r.getOrder().name());
        e.setMessage(r.getMessage());
        e.setExpiresAt(r.getExpiresAt());
        e.setReminderEveryDays(r.getReminderEveryDays());
        e.setReminderPrompts(r.getReminderPrompts());
        e.setLastReminderAt(r.getLastReminderAt());
        e.setRecordModel(r.getRecordModel());
        e.setRecordId(r.getRecordId());
        e.setFinalDocumentId(r.getFinalDocumentId());
        e.setFinalSha256(r.getFinalSha256());
        e.setSentAt(r.getSentAt());
        e.setCompletedAt(r.getCompletedAt());
        e.setCanceledAt(r.getCanceledAt());
        e.setCancelReason(r.getCancelReason());
        e.setNeedsAttention(r.isNeedsAttention());
        e.setAttentionReason(r.getAttentionReason());
        e.setBuildAttempts(r.getBuildAttempts());
        e.setCreatedAt(r.getCreatedAt());
        e.setCreatedBy(r.getCreatedBy());
        e.setCreatedByUserId(r.getCreatedByUserId());
        return e;
    }

    public SignerEntity toEntity(UUID requestId, SignerItem s) {
        SignerEntity e = new SignerEntity();
        e.setId(s.getId());
        e.setRequestId(requestId);
        e.setRoleName(s.getRoleName());
        e.setSequence(s.getSequence());
        e.setApproverOnly(s.isApproverOnly());
        e.setPartnerId(s.getPartnerId());
        e.setUserId(s.getUserId());
        e.setName(s.getName());
        e.setEmail(s.getEmail());
        e.setPhone(s.getPhone());
        e.setChannel(s.getChannel().name());
        e.setTokenHash(s.getTokenHash());
        e.setTokenExpiresAt(s.getTokenExpiresAt());
        e.setStatus(s.getStatus().name());
        e.setViewedAt(s.getViewedAt());
        e.setSignedAt(s.getSignedAt());
        e.setRefusedAt(s.getRefusedAt());
        e.setRefuseReason(s.getRefuseReason());
        e.setSignedIp(s.getSignedIp());
        e.setSignedUserAgent(s.getSignedUserAgent());
        e.setSubmissionDigest(s.getSubmissionDigest());
        e.setLinkIssued(s.isLinkIssued());
        e.setOperatorUserId(s.getOperatorUserId());
        e.setIdChecked(s.isIdChecked());
        e.setIdNote(s.getIdNote());
        return e;
    }

    public RequestFieldEntity toEntity(UUID requestId, RequestField f) {
        RequestFieldEntity e = new RequestFieldEntity();
        e.setId(f.getId());
        e.setRequestId(requestId);
        e.setSignerItemId(f.getSignerItemId());
        var g = f.getGeometry();
        e.setPage(g.page());
        e.setX(g.x());
        e.setY(g.y());
        e.setWidth(g.width());
        e.setHeight(g.height());
        e.setFieldType(f.getType().name());
        e.setRequired(f.isRequired());
        e.setLabel(f.getLabel());
        e.setPlaceholder(f.getPlaceholder());
        e.setAutoFill(f.getAutoFill().name());
        e.setValueText(f.getValueText());
        e.setValueBool(f.getValueBool());
        e.setValueImage(f.getValueImage() == null ? null : Base64.getEncoder().encodeToString(f.getValueImage()));
        return e;
    }

    // ------------------------------------------------------------------ events

    public SignEvent toDomain(EventEntity e) {
        return new SignEvent(e.getId(), e.getRequestId(), e.getSignerItemId(), EventType.valueOf(e.getEventType()), e.getOccurredAt(),
                e.getIp(), e.getUserAgent(), e.getDetails(), e.getPrevHash(), e.getEventHash());
    }

    public EventEntity toEntity(SignEvent ev, long seq) {
        EventEntity e = new EventEntity();
        e.setId(ev.getId());
        e.setRequestId(ev.getRequestId());
        e.setSignerItemId(ev.getSignerItemId());
        e.setEventType(ev.getType().name());
        e.setOccurredAt(ev.getOccurredAt());
        e.setIp(ev.getIp());
        e.setUserAgent(ev.getUserAgent());
        e.setDetails(ev.getDetails());
        e.setPrevHash(ev.getPrevHash());
        e.setEventHash(ev.getEventHash());
        e.setSeq(seq);
        return e;
    }
}
