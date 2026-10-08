package com.bradox.erp.sign.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.sign.domain.core.entity.SignEvent;
import com.bradox.erp.sign.domain.core.valueobject.EventType;
import com.bradox.erp.sign.service.domain.ports.output.repository.EventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Writes the hash-chained event log (BR-SIG-08) and mirrors state changes to the platform audit log. */
@Component
class SignRecorder {

    static final String AUDIT_MODEL = "sign.request";

    private final EventRepository events;
    private final AuditLogPort audit;

    SignRecorder(EventRepository events, AuditLogPort audit) {
        this.events = events;
        this.audit = audit;
    }

    void event(UUID requestId, UUID signerId, EventType type, Instant at, String ip, String userAgent, String details) {
        events.append(SignEvent.chain(requestId, signerId, type, at, ip, userAgent, details, events.lastHash(requestId)));
    }

    void audit(CompanyId companyId, UUID requestId, String message, Map<String, Object> details) {
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, requestId, message, details);
    }
}
