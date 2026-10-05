package com.fairhire.controllers;

import com.fairhire.services.EvaluationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final EvaluationService evaluationService;

    public ReportController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        return ResponseEntity.ok(evaluationService.computeMetrics());
    }

    @GetMapping("/experiments")
    public ResponseEntity<?> getExperiments() {
        return ResponseEntity.ok(evaluationService.getAllExperiments());
    }

    @PostMapping("/experiments")
    public ResponseEntity<?> addExperiment(@RequestBody Map<String, Object> body) {
        String type = (String) body.getOrDefault("type", "scoring_benchmark");
        String ds = (String) body.getOrDefault("dataset_name", "FairHire-Dataset-v1");
        String name = (String) body.get("metric_name");
        Double val = body.get("metric_value") instanceof Number n ? n.doubleValue() : 0.0;

        if (name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "metric_name is required."));
        }
        var exp = evaluationService.recordExperiment(type, ds, name, val);
        return ResponseEntity.status(201).body(Map.of("message", "Experiment recorded", "experiment", exp));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportResults(@RequestParam(value = "job_id", required = false) Long jobId) {
        String csv = evaluationService.generateCsvReport(jobId);
        byte[] bytes = csv.getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fairhire_report_" + (jobId != null ? jobId : "all") + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
