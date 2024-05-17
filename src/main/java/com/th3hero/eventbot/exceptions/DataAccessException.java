package com.th3hero.eventbot.exceptions;


/**
 * Exception thrown when there is an unexpected error retrieving information from an object.
 * For example, retrieving an expected field on a Modal.
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message) {
        super(message);
    }
}
