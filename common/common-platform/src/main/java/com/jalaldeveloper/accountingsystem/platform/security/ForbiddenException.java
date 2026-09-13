package com.jalaldeveloper.accountingsystem.platform.security;

import com.jalaldeveloper.accountingsystem.domain.exception.DomainException;

/**
 * Thrown by {@link PermissionAspect} when the current user lacks one of the
 * required permissions. Mapped to HTTP 403 by {@code PlatformExceptionHandler}.
 */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
