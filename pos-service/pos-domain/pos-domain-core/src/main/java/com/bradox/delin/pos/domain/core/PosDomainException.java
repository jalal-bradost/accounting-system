package com.bradox.delin.pos.domain.core;

import com.bradox.delin.domain.exception.DomainException;

public class PosDomainException extends DomainException {

    public PosDomainException(String message) {
        super(message);
    }

    public PosDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
