package com.bradox.erp.timesheet.domain.core.exception;

import com.bradox.erp.domain.exception.DomainException;

public class TimesheetDomainException extends DomainException {

    public TimesheetDomainException(String message) {
        super(message);
    }

    public TimesheetDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
