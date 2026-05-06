package com.jalaldeveloper.accountingsystem.platform.audit;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.util.Map;
import java.util.UUID;

/**
 * Application-facing API for writing audit log entries. CRUD events are produced
 * automatically by the JPA listener; this port is for explicit business events.
 */
public interface AuditLogPort {

    /** Records an explicit business event (state transition, manual action, etc). */
    void recordBusinessEvent(CompanyId companyId,
                             String modelName,
                             UUID recordId,
                             String message,
                             Map<String, Object> changes);

    /** Records a CRUD event. Used by the JPA listener but also available manually. */
    void recordCrud(CompanyId companyId,
                    AuditAction action,
                    String modelName,
                    UUID recordId,
                    Map<String, Object> changes);
}
