package com.electrician.tracker.service.exception;

/**
 * Thrown when a unique name constraint (e.g. product name) is violated.
 * The message is a resource bundle key, translated by the UI layer.
 */
public class DuplicateNameException extends RuntimeException {

    public DuplicateNameException(String messageKey) {
        super(messageKey);
    }
}
