package com.fairhire.controllers;

import com.fairhire.services.MatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/matching")
@CrossOrigin(origins = "*")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<?> runMatching(@PathVariable Long jobId, @RequestBody(required = false) Map<String, Object> body) {
        try {
            Double kw = body != null && body.containsKey("keyword_weight")
                    ? ((Number) body.get("keyword_weight")).doubleValue() : 0.3;
            Double sem = body != null && body.containsKey("semantic_weight")
                    ? ((Number) body.get("semantic_weight")).doubleValue() : 0.7;

            var results = matchingService.runJobMatching(jobId, kw, sem);
            return ResponseEntity.ok(Map.of(
                    "message", "Matching executed successfully.",
                    "job_id", jobId,
                    "total_ranked", results.size(),
                    "results", results
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{jobId}/results")
    public ResponseEntity<?> getResults(@PathVariable Long jobId) {
        try {
            var results = matchingService.getJobMatchResults(jobId);
            return ResponseEntity.ok(Map.of(
                    "job_id", jobId,
                    "total_candidates", results.size(),
                    "results", results
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMatch(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(matchingService.getMatchDetail(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
