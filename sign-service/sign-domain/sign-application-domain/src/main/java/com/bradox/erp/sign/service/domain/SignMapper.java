package com.bradox.erp.sign.service.domain;

import com.bradox.erp.sign.domain.core.entity.RequestField;
import com.bradox.erp.sign.domain.core.entity.SignEvent;
import com.bradox.erp.sign.domain.core.entity.SignRequest;
import com.bradox.erp.sign.domain.core.entity.SignerItem;
import com.bradox.erp.sign.domain.core.entity.Template;
import com.bradox.erp.sign.domain.core.entity.TemplateField;
import com.bradox.erp.sign.domain.core.entity.TemplateRole;
import com.bradox.erp.sign.service.domain.dto.EventResponse;
import com.bradox.erp.sign.service.domain.dto.FieldDto;
import com.bradox.erp.sign.service.domain.dto.RequestResponse;
import com.bradox.erp.sign.service.domain.dto.RequestSummaryResponse;
import com.bradox.erp.sign.service.domain.dto.RoleDto;
import com.bradox.erp.sign.service.domain.dto.SignerResponse;
import com.bradox.erp.sign.service.domain.dto.TemplateResponse;
import com.bradox.erp.sign.service.domain.dto.TemplateSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class SignMapper {

    private SignMapper() {
    }

    static RoleDto role(TemplateRole r) {
        return new RoleDto(r.getId(), r.getName(), r.getColor(), r.getSequence(), r.isApproverOnly(), r.getDefaultPartnerId());
    }

    static FieldDto field(TemplateField f) {
        var g = f.getGeometry();
        return new FieldDto(f.getId(), f.getRoleId().toString(), g.page(), g.x(), g.y(), g.width(), g.height(), f.getType().name(),
                f.isRequired(), f.getLabel(), f.getPlaceholder(), f.getAutoFill().name());
    }

    static FieldDto field(RequestField f) {
        var g = f.getGeometry();
        return new FieldDto(f.getId(), f.getSignerItemId().toString(), g.page(), g.x(), g.y(), g.width(), g.height(),
                f.getType().name(), f.isRequired(), f.getLabel(), f.getPlaceholder(), f.getAutoFill().name());
    }

    static TemplateSummaryResponse summary(Template t) {
        return new TemplateSummaryResponse(t.getId(), t.getName(), t.getCategory(), t.getPageCount(), t.isActive(), t.getRoles().size(),
                t.getCreatedAt());
    }

    static TemplateResponse template(Template t, long sentRequests) {
        return new TemplateResponse(t.getId(), t.getName(), t.getCategory(), t.getDocumentId(), t.getDocumentSha256(), t.getPageCount(),
                t.isActive(), t.getDefaultValidityDays(), t.getDefaultMessage(),
                t.getRoles().stream().map(SignMapper::role).toList(), t.getFields().stream().map(SignMapper::field).toList(),
                sentRequests, t.getCreatedAt(), t.getCreatedBy());
    }

    static SignerResponse signer(SignerItem s, boolean audit) {
        return new SignerResponse(s.getId(), s.getRoleName(), s.getSequence(), s.isApproverOnly(), s.getName(), s.getEmail(),
                s.getPhone(), s.getPartnerId(), s.getUserId(), s.getStatus().name(), s.getChannel().name(), s.isLinkIssued(),
                s.getViewedAt(), s.getSignedAt(), s.getRefusedAt(), s.getRefuseReason(), audit ? s.getSignedIp() : null,
                audit ? s.getSignedUserAgent() : null);
    }

    static RequestSummaryResponse summary(SignRequest r, Instant now) {
        String current = r.ordered().stream().filter(SignerItem::isActive).map(SignerItem::getName).findFirst().orElse(null);
        return new RequestSummaryResponse(r.getId(), r.getReference(), r.getName(), r.getStatus().name(), r.signedCount(),
                r.getSigners().size(), current, r.getExpiresAt(), r.getSentAt(), r.getCompletedAt(), r.isNeedsAttention(),
                r.reminderDue(now), r.getRecordModel(), r.getRecordId(), r.getCreatedBy(), r.getCreatedAt());
    }

    static RequestResponse request(SignRequest r, Instant now, boolean audit, boolean canEdit, boolean canCancel) {
        return new RequestResponse(r.getId(), r.getReference(), r.getName(), r.getStatus().name(), r.getOrder().name(), r.getMessage(),
                r.getTemplateId(), r.getSourceDocumentId(), r.getSourceSha256(), r.getPageCount(), r.getExpiresAt(),
                r.getReminderEveryDays(), r.getReminderPrompts(), r.reminderDue(now), r.getRecordModel(), r.getRecordId(),
                r.getFinalDocumentId(), r.getFinalSha256(), r.getSentAt(), r.getCompletedAt(), r.getCanceledAt(), r.getCancelReason(),
                r.isNeedsAttention(), r.getAttentionReason(), r.getCreatedBy(), r.getCreatedAt(), r.signedCount(),
                r.ordered().stream().map(s -> signer(s, audit)).toList(),
                r.getFields().stream().map(SignMapper::field).toList(), r.hasRepeatedSigner(), canEdit, canCancel);
    }

    static EventResponse event(SignEvent e, Map<UUID, String> signerNames, boolean audit) {
        return new EventResponse(e.getId(), e.getType().name(), e.getOccurredAt(), e.getSignerItemId(),
                e.getSignerItemId() == null ? null : signerNames.get(e.getSignerItemId()), audit ? e.getIp() : null,
                audit ? e.getUserAgent() : null, e.getDetails());
    }
}
