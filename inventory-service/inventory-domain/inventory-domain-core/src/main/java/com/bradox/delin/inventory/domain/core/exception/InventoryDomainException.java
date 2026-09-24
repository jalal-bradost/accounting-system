package com.bradox.delin.inventory.domain.core.exception;

import com.bradox.delin.domain.exception.DomainException;

/**
 * Thrown when an inventory aggregate rejects an operation because a domain invariant
 * (e.g. negative stock when not allowed, invalid state transition) would be violated.
 */
public class InventoryDomainException extends DomainException {

    public InventoryDomainException(String message) {
        super(message);
    }

    public InventoryDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public InventoryDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
