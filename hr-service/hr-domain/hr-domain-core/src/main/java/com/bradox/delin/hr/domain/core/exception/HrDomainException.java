package com.bradox.delin.hr.domain.core.exception;

import com.bradox.delin.domain.exception.DomainException;

public class HrDomainException extends DomainException {

    public HrDomainException(String message) {
        super(message);
    }

    public HrDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public HrDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
