package com.bradox.erp.sign.service.domain;

import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.security.AuthorizationPort;
import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Who is acting, what they may do, and "now". The one place the signed-in user is read. */
@Component
class SignAccess {

    private final CompanyContext context;
    private final AuthorizationPort authorization;
    private final Clock clock;

    @Autowired
    SignAccess(CompanyContext context, AuthorizationPort authorization, ObjectProvider<Clock> clock) {
        this(context, authorization, clock.getIfAvailable(Clock::systemUTC));
    }

    SignAccess(CompanyContext context, AuthorizationPort authorization, Clock clock) {
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
            throw new ForbiddenException("error.sign.forbidden", new Object[]{permission}, "You do not have permission to do that");
        }
    }

    Optional<UUID> userId() {
        return context.currentUser().map(UserId::getId);
    }

    UUID requireUserId() {
        return userId().orElseThrow(() -> new ForbiddenException("error.sign.forbidden", null, "You must be signed in"));
    }

    String actorLabel() {
        return context.currentUserDisplay();
    }

    Instant now() {
        return clock.instant();
    }

    Clock clock() {
        return clock;
    }
}
