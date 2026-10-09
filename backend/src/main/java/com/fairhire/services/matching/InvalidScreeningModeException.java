package com.fairhire.services.matching;

/**
 * Exception thrown when an unrecognized screening mode is passed in a match request.
 */
public class InvalidScreeningModeException extends RuntimeException {
    public InvalidScreeningModeException(String message) {
        super(message);
    }
}
