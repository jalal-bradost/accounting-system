package com.bradox.erp.sign.domain.core.exception;

import com.bradox.erp.domain.exception.DomainException;

public class SignDomainException extends DomainException {

    public SignDomainException(String message) {
        super(message);
    }

    public SignDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
