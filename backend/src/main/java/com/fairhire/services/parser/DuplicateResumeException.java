package com.fairhire.services.parser;

public class DuplicateResumeException extends RuntimeException {
    private final String sha256;
    private final Long existingResumeId;

    public DuplicateResumeException(String message, String sha256, Long existingResumeId) {
        super(message);
        this.sha256 = sha256;
        this.existingResumeId = existingResumeId;
    }

    public String getSha256() {
        return sha256;
    }

    public Long getExistingResumeId() {
        return existingResumeId;
    }
}
