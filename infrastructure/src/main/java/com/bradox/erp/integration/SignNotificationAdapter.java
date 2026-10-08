package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.activity.ActivityApplicationService;
import com.bradox.erp.platform.activity.ActivityKind;
import com.bradox.erp.platform.activity.CreateActivityCommand;
import com.bradox.erp.sign.service.domain.ports.output.SignNotificationPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Phase 1 delivery (D6): an item in the sender's activity inbox. SMTP or messaging adapters can be added beside this one. */
@Component
public class SignNotificationAdapter implements SignNotificationPort {

    static final String MODEL = "sign.request";

    private final ActivityApplicationService activities;

    public SignNotificationAdapter(ActivityApplicationService activities) {
        this.activities = activities;
    }

    @Override
    public void notifyUser(CompanyId companyId, UUID userId, UUID requestId, String subject, String body) {
        CreateActivityCommand cmd = new CreateActivityCommand();
        cmd.setCompanyId(companyId.getId());
        cmd.setModelName(MODEL);
        cmd.setRecordId(requestId);
        cmd.setKind(ActivityKind.ACTIVITY_TODO);
        cmd.setSubject(subject.length() > 200 ? subject.substring(0, 197) + "…" : subject);
        cmd.setBody(body);
        cmd.setAssigneeId(userId.toString());
        activities.create(cmd);
    }
}
