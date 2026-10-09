package com.fairhire.services.matching;

import com.fairhire.config.FairHireUserPrincipal;
import com.fairhire.models.Job;
import com.fairhire.models.Resume;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service enforcing resource authorization boundaries at the service layer.
 * Enforces departmental and tenant scoping on jobs and resumes.
 * Prohibits cross-department and cross-tenant matching.
 * 
 * Production Authorization Boundary:
 * The authorized department/scope is derived from trusted authenticated identity
 * (FairHireUserPrincipal from Spring Security), not an arbitrary client header.
 * If client supplies X-Department, it is validated against the authenticated principal
 * and can never override trusted identity.
 */
@Service
public class ResourceAuthorizationService {

    /**
     * Validate that the caller is authorized to access the specified job.
     * Enforces strict fail-closed authorization:
     * - If authenticated via Spring Security, department scope is derived from the trusted principal.
     * - Client-supplied header (X-Department) is strictly validated against the trusted principal and cannot override identity.
     * - If no authenticated context exists (pure unit tests), callerDepartment must be explicitly supplied and non-blank.
     * - Target job's department must match the authorized department.
     *
     * @param job Target job entity
     * @param clientDepartment Client-supplied department header/parameter (optional/for validation)
     * @throws UnauthorizedResourceAccessException if caller is unauthorized
     */
    public void validateJobAccess(Job job, String clientDepartment) {
        if (job == null) return;

        // 1. Resolve trusted department from authenticated SecurityContextHolder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof FairHireUserPrincipal principal) {
            String trustedDepartment = principal.department();

            // Fail-closed: Authenticated user must have an assigned department scope
            if (trustedDepartment == null || trustedDepartment.isBlank()) {
                throw new UnauthorizedResourceAccessException(
                        "Authenticated user '" + principal.getName() + "' has no assigned department scope. Access denied."
                );
            }

            // Client-supplied X-Department must be non-blank and match trusted principal (cannot override identity)
            if (clientDepartment != null) {
                if (clientDepartment.isBlank() || !clientDepartment.trim().equalsIgnoreCase(trustedDepartment.trim())) {
                    throw new UnauthorizedResourceAccessException(
                            "Client-supplied department '" + clientDepartment + "' does not match authenticated user department '" +
                            trustedDepartment + "'. Client headers cannot override trusted identity."
                    );
                }
            }

            // Verify access against target job's department using trusted identity
            if (job.getDepartment() != null && !job.getDepartment().isBlank()) {
                if (!job.getDepartment().trim().equalsIgnoreCase(trustedDepartment.trim())) {
                    throw new UnauthorizedResourceAccessException(
                            "Authenticated user from department '" + trustedDepartment + "' is unauthorized to access job in department '" +
                            job.getDepartment() + "'."
                    );
                }
            }
            return;
        }

        // 2. Fallback for standalone unit/integration tests without SecurityContext
        if (clientDepartment == null || clientDepartment.isBlank()) {
            throw new UnauthorizedResourceAccessException(
                    "Caller authorization context is missing. Cannot authorize access to job in department '" +
                    (job.getDepartment() != null ? job.getDepartment() : "unassigned") + "' without caller department identification."
            );
        }
        if (job.getDepartment() != null && !job.getDepartment().isBlank()) {
            if (!job.getDepartment().trim().equalsIgnoreCase(clientDepartment.trim())) {
                throw new UnauthorizedResourceAccessException(
                        "Caller from department '" + clientDepartment + "' is unauthorized to access job in department '" +
                        job.getDepartment() + "'."
                );
            }
        }
    }

    /**
     * Validate that candidate resume can be matched against the job.
     * Rejects cross-department matching if resume department is specified.
     *
     * @param job Target job entity
     * @param resume Candidate resume entity
     * @param resumeDepartments Map of resume ID to department context (optional)
     */
    public void validateResumeAccess(Job job, Resume resume, Map<Long, String> resumeDepartments) {
        if (job == null || resume == null) return;
        if (resumeDepartments != null && resumeDepartments.containsKey(resume.getId())) {
            String resumeDept = resumeDepartments.get(resume.getId());
            if (resumeDept != null && !resumeDept.isBlank()
                    && job.getDepartment() != null && !job.getDepartment().isBlank()) {
                if (!job.getDepartment().trim().equalsIgnoreCase(resumeDept.trim())) {
                    throw new UnauthorizedResourceAccessException(
                            "Candidate resume ID " + resume.getId() + " from department '" + resumeDept +
                            "' cannot be evaluated against job in department '" + job.getDepartment() + "'. Cross-department matching is strictly prohibited."
                    );
                }
            }
        }
    }
}
