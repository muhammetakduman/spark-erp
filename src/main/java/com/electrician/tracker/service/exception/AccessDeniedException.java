package com.electrician.tracker.service.exception;

/**
 * Thrown when the logged-in user's role does not allow an operation (e.g. a
 * MANAGER deleting a site). The message is a resource bundle key, translated
 * by the UI layer.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String messageKey) {
        super(messageKey);
    }
}
