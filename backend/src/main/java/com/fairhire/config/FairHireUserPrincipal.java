package com.fairhire.config;

import java.io.Serializable;
import java.security.Principal;

/**
 * Immutable trusted principal representing an authenticated FairHire user.
 * Carries verified user identity, role, and department scope extracted from JWT claims.
 */
public record FairHireUserPrincipal(
        Long userId,
        String email,
        String role,
        String department
) implements Principal, Serializable {

    @Override
    public String getName() {
        return email != null && !email.isBlank()
                ? email
                : (userId != null ? String.valueOf(userId) : "anonymous");
    }
}
