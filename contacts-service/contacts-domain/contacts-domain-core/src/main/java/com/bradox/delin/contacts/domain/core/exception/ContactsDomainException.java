package com.bradox.delin.contacts.domain.core.exception;

import com.bradox.delin.domain.exception.DomainException;

public class ContactsDomainException extends DomainException {

    public ContactsDomainException(String message) {
        super(message);
    }

    public ContactsDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContactsDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
