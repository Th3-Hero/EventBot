package com.th3hero.eventbot.exceptions;

/**
 * Thrown when the request is unable to process the response.
 */
public class UnsupportedResponseException extends RuntimeException {
    public UnsupportedResponseException(String message) {
        super(message);
    }
}
