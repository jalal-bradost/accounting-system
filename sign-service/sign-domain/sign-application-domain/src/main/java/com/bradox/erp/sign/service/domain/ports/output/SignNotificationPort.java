package com.bradox.erp.sign.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.UUID;

/** Phase 1 delivers in-app only; an email adapter can be added later without domain changes (D6). */
public interface SignNotificationPort {

    void notifyUser(CompanyId companyId, UUID userId, UUID requestId, String subject, String body);
}
