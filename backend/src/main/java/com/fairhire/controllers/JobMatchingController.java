package com.fairhire.controllers;

import com.fairhire.dto.MatchBatchResponseDto;
import com.fairhire.dto.MatchDetailResponseDto;
import com.fairhire.dto.MatchRequestDto;
import com.fairhire.dto.MatchResponseDto;
import com.fairhire.services.matching.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Phase 4 deterministic canonical skill matching engine.
 * Endpoints:
 * - POST /api/v1/jobs/{jobId}/matches
 * - GET  /api/v1/jobs/{jobId}/matches
 * - GET  /api/v1/jobs/{jobId}/matches/{resumeId}
 */
@RestController
@CrossOrigin(origins = "*")
public class JobMatchingController {

    private final SkillCoverageMatchingService matchingService;

    public JobMatchingController(SkillCoverageMatchingService matchingService) {
        this.matchingService = matchingService;
    }

    /**
     * Recruiter-triggered deterministic matching execution.
     */
    @PostMapping({"/api/v1/jobs/{jobId}/matches", "/api/jobs/{jobId}/matches"})
    public ResponseEntity<?> runJobMatches(
            @PathVariable("jobId") Long jobId,
            @RequestBody(required = false) MatchRequestDto request,
            @RequestHeader(value = "X-Department", required = false) String headerDepartment
    ) {
        try {
            List<Long> resumeIds = request != null ? request.getResumeIds() : null;
            String callerDept = (request != null && request.getDepartment() != null && !request.getDepartment().isBlank())
                    ? request.getDepartment()
                    : headerDepartment;
            Map<Long, String> resumeDepts = request != null ? request.getResumeDepartments() : null;
            String screeningMode = request != null ? request.getScreeningMode() : null;
            String matchingMethod = request != null ? request.getMatchingMethod() : null;

            MatchBatchResponseDto result = matchingService.matchJob(
                    jobId, resumeIds, callerDept, resumeDepts, screeningMode, matchingMethod
            );
            return ResponseEntity.ok(result);
        } catch (InvalidScreeningModeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", 400,
                    "error", "INVALID_SCREENING_MODE",
                    "message", e.getMessage()
            ));
        } catch (InvalidMatchingMethodException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "status", 422,
                    "error", "INVALID_MATCHING_METHOD",
                    "message", e.getMessage()
            ));
        } catch (MatchingRequirementsNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "status", 422,
                    "error", "MATCHING_REQUIREMENTS_NOT_FOUND",
                    "message", e.getMessage()
            ));
        } catch (BatchSizeLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "status", 422,
                    "error", "BATCH_SIZE_LIMIT_EXCEEDED",
                    "message", e.getMessage()
            ));
        } catch (UnauthorizedResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403,
                    "error", "UNAUTHORIZED_RESOURCE_ACCESS",
                    "message", e.getMessage()
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", 404,
                    "error", "RESOURCE_NOT_FOUND",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", 500,
                    "error", "INTERNAL_SERVER_ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Retrieve all deterministic match results for a job.
     */
    @GetMapping({"/api/v1/jobs/{jobId}/matches", "/api/jobs/{jobId}/matches"})
    public ResponseEntity<?> getJobMatches(
            @PathVariable("jobId") Long jobId,
            @RequestHeader(value = "X-Department", required = false) String headerDepartment
    ) {
        try {
            List<MatchResponseDto> results = matchingService.getJobMatches(jobId, headerDepartment);
            return ResponseEntity.ok(Map.of(
                    "job_id", jobId,
                    "total_evaluated", results.size(),
                    "results", results
            ));
        } catch (UnauthorizedResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403,
                    "error", "UNAUTHORIZED_RESOURCE_ACCESS",
                    "message", e.getMessage()
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", 404,
                    "error", "RESOURCE_NOT_FOUND",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", 500,
                    "error", "INTERNAL_SERVER_ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Retrieve detailed decomposed match result for a single candidate resume.
     */
    @GetMapping({"/api/v1/jobs/{jobId}/matches/{resumeId}", "/api/jobs/{jobId}/matches/{resumeId}"})
    public ResponseEntity<?> getMatchDetail(
            @PathVariable("jobId") Long jobId,
            @PathVariable("resumeId") Long resumeId,
            @RequestHeader(value = "X-Department", required = false) String headerDepartment
    ) {
        try {
            MatchDetailResponseDto detail = matchingService.getMatchDetail(jobId, resumeId, headerDepartment);
            return ResponseEntity.ok(detail);
        } catch (UnauthorizedResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403,
                    "error", "UNAUTHORIZED_RESOURCE_ACCESS",
                    "message", e.getMessage()
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", 404,
                    "error", "RESOURCE_NOT_FOUND",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", 500,
                    "error", "INTERNAL_SERVER_ERROR",
                    "message", e.getMessage()
            ));
        }
    }
}
