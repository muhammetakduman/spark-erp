package com.electrician.tracker.service.exception;

/**
 * Thrown when the same job + employee + date attendance entry already exists.
 * The message is a resource bundle key, translated by the UI layer.
 */
public class DuplicateAttendanceException extends RuntimeException {

    public DuplicateAttendanceException(String messageKey) {
        super(messageKey);
    }
}
