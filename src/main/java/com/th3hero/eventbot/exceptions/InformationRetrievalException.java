package com.th3hero.eventbot.exceptions;


/**
 * Exception thrown when there is an unexpected error retrieving information from an object.
 * For example, retrieving an expected field on a Modal.
 */
public class InformationRetrievalException extends RuntimeException {
    public InformationRetrievalException(String message) {
        super(message);
    }
}
