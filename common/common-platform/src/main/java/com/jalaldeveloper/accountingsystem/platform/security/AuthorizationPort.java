package com.jalaldeveloper.accountingsystem.platform.security;

import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;

import java.util.Set;

/**
 * Authorization SPI. Determines whether the given user is allowed to perform a set
 * of permissions. Default implementation in this module is permissive (returns
 * {@code true}) so existing controllers keep working in dev and tests; a real
 * Spring Security adapter can replace it later by defining a higher-precedence
 * {@code @Primary} bean.
 */
public interface AuthorizationPort {

    /** True iff the user holds every permission in {@code requiredPermissions} (logical AND). */
    boolean hasAll(UserId userId, Set<String> requiredPermissions);

    /** True iff the user holds at least one of the given permissions (logical OR). */
    boolean hasAny(UserId userId, Set<String> requiredPermissions);
}
