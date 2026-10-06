package com.bradox.erp.assistant.provider;

/**
 * Raised when the local model runtime cannot complete a generation request.
 */
public class AIProviderException extends RuntimeException {

    public AIProviderException(String message) {
        super(message);
    }

    public AIProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
