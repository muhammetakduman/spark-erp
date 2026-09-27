package com.electrician.tracker.service.exception;

/**
 * Thrown when trying to delete an entity that other records still refer to.
 * The message is a resource bundle key, translated by the UI layer; its
 * {0} placeholder receives {@link #getReferenceCount()}.
 */
public class ReferencedEntityException extends RuntimeException {

    private final long referenceCount;

    public ReferencedEntityException(String messageKey, long referenceCount) {
        super(messageKey);
        this.referenceCount = referenceCount;
    }

    public long getReferenceCount() {
        return referenceCount;
    }
}
