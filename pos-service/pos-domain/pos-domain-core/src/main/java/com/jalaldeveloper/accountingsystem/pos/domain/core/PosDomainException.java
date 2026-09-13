package com.jalaldeveloper.accountingsystem.pos.domain.core;

import com.jalaldeveloper.accountingsystem.domain.exception.DomainException;

public class PosDomainException extends DomainException {

    public PosDomainException(String message) {
        super(message);
    }

    public PosDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
