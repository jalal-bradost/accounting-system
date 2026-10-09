package com.bradox.erp.project.domain.core.exception;

import com.bradox.erp.domain.exception.DomainException;

public class ProjectDomainException extends DomainException {

    public ProjectDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
