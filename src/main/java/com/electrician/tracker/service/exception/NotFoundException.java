package com.electrician.tracker.service.exception;

/**
 * Thrown when an entity looked up by id does not exist.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String messageKey) {
        super(messageKey);
    }
}
