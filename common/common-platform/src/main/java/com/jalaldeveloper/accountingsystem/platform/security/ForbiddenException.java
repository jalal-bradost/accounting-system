package com.jalaldeveloper.accountingsystem.platform.security;

/**
 * Thrown by {@link PermissionAspect} when the current user lacks one of the
 * required permissions. Mapped to HTTP 403 by {@code PlatformExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
