package com.electrician.tracker.service.exception;

/**
 * Thrown when a form/service input fails a business validation rule.
 * The message is a resource bundle key, translated by the UI layer.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String messageKey) {
        super(messageKey);
    }
}
