package com.fairhire.services.parser;

public class InvalidResumeUploadException extends RuntimeException {
    public InvalidResumeUploadException(String message) {
        super(message);
    }

    public InvalidResumeUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
