package com.jalaldeveloper.accountingsystem.application.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class LocalizedResponseStatusException extends ResponseStatusException {

    private final String messageKey;
    private final Object[] messageArgs;

    public LocalizedResponseStatusException(HttpStatusCode status, String messageKey, String defaultMessage) {
        this(status, messageKey, null, defaultMessage);
    }

    public LocalizedResponseStatusException(
            HttpStatusCode status, String messageKey, Object[] messageArgs, String defaultMessage) {
        super(status, defaultMessage);
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
