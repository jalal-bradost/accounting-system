package com.bradox.delin.domain.exception;

public class DomainException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public DomainException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = null;
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
        this.messageKey = null;
        this.messageArgs = null;
    }

    public DomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(defaultMessage);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }
}
