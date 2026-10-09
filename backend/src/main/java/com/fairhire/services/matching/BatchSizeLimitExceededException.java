package com.fairhire.services.matching;

public class BatchSizeLimitExceededException extends RuntimeException {
    public BatchSizeLimitExceededException(String message) {
        super(message);
    }
}
