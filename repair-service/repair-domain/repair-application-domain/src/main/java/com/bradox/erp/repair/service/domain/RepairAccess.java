package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.security.AuthorizationPort;
import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/** Who is acting, what they may do, and "now". The one place the signed-in user is read. */
@Component
class RepairAccess {

    private final CompanyContext context;
    private final AuthorizationPort authorization;
    private final Clock clock;

    @Autowired
    RepairAccess(CompanyContext context, AuthorizationPort authorization, ObjectProvider<Clock> clock) {
        this(context, authorization, clock.getIfAvailable(Clock::systemUTC));
    }

    RepairAccess(CompanyContext context, AuthorizationPort authorization, Clock clock) {
        this.context = context;
        this.authorization = authorization;
        this.clock = clock;
    }

    boolean can(String permission) {
        UserId user = context.currentUser().orElse(null);
        return authorization.hasAll(user, Set.of(permission));
    }

    void require(String permission) {
        if (!can(permission)) {
            throw new ForbiddenException("error.repair.forbidden", new Object[]{permission}, "You do not have permission to do that");
        }
    }

    String actorLabel() {
        return context.currentUserDisplay();
    }

    Instant now() {
        return clock.instant();
    }
}
