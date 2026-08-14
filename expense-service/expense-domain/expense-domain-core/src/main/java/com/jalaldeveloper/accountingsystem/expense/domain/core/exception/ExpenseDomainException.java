package com.jalaldeveloper.accountingsystem.expense.domain.core.exception;

import com.jalaldeveloper.accountingsystem.domain.exception.DomainException;

public class ExpenseDomainException extends DomainException {

    public ExpenseDomainException(String message) {
        super(message);
    }

    public ExpenseDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
