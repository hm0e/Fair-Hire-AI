package com.fairhire.controllers;

import com.fairhire.services.BiasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class BiasController {

    private final BiasService biasService;

    public BiasController(BiasService biasService) {
        this.biasService = biasService;
    }

    @PostMapping({"/api/bias/analyze", "/api/analyze-jd"})
    public ResponseEntity<?> analyze(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", body.get("jd_text"));
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text is required."));
        }
        Map<String, Object> result = biasService.analyzeText(text);
        if ("SERVICE_UNAVAILABLE".equals(result.get("status"))) {
            return ResponseEntity.status(503).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/bias/{jobId}")
    public ResponseEntity<?> getBiasReport(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(biasService.getJobBiasReport(jobId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/bias/rewrite")
    public ResponseEntity<?> rewrite(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", body.get("jd_text"));
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text is required."));
        }
        return ResponseEntity.ok(biasService.rewriteText(text));
    }
}
