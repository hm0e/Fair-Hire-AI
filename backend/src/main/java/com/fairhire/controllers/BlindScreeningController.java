package com.fairhire.controllers;

import com.fairhire.services.BlindScreeningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/blind-screening")
@CrossOrigin(origins = "*")
public class BlindScreeningController {

    private final BlindScreeningService blindScreeningService;

    public BlindScreeningController(BlindScreeningService blindScreeningService) {
        this.blindScreeningService = blindScreeningService;
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<?> runBlindScreening(@PathVariable Long jobId) {
        try {
            var data = blindScreeningService.getBlindComparison(jobId);
            return ResponseEntity.ok(Map.of(
                    "message", "Blind screening comparison generated successfully.",
                    "data", data
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getBlindComparison(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(blindScreeningService.getBlindComparison(jobId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
