package com.bradox.erp.assistant.tools.support;

import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.security.AuthorizationPort;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Permission checks inside multi-entity tools. Sections the user cannot see
 * must never leak data — return {@link #RESTRICTED} or omit the section.
 */
@Component
public class EntityAccess {

    public static final String RESTRICTED = "restricted";

    private final AuthorizationPort authorizationPort;

    public EntityAccess(AuthorizationPort authorizationPort) {
        this.authorizationPort = authorizationPort;
    }

    public boolean can(UserId userId, String permission) {
        return userId != null && authorizationPort.hasAny(userId, Set.of(permission));
    }

    public boolean canAny(UserId userId, Set<String> permissions) {
        return userId != null && authorizationPort.hasAny(userId, permissions);
    }

    /**
     * Runs {@code supplier} when permitted; otherwise returns {@link #RESTRICTED}.
     * Exceptions from the supplier are rethrown.
     */
    public Object whenPermitted(UserId userId, String permission, Supplier<Object> supplier) {
        if (!can(userId, permission)) {
            return RESTRICTED;
        }
        return supplier.get();
    }
}
