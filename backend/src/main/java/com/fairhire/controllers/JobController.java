package com.fairhire.controllers;

import com.fairhire.models.Job;
import com.fairhire.services.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody Map<String, Object> body) {
        try {
            String title = String.valueOf(body.getOrDefault("title", ""));
            String description = String.valueOf(body.getOrDefault("description", ""));
            String requirements = body.get("requirements") != null ? String.valueOf(body.get("requirements")) : "";

            Job job = jobService.createJob(title, description, requirements, 1L);
            return ResponseEntity.status(201).body(jobService.formatJobDto(job));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(jobService.getJobById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String title = body.get("title") != null ? String.valueOf(body.get("title")) : null;
            String description = body.get("description") != null ? String.valueOf(body.get("description")) : null;
            String requirements = body.get("requirements") != null ? String.valueOf(body.get("requirements")) : null;

            var updated = jobService.updateJob(id, title, description, requirements);
            return ResponseEntity.ok(Map.of("message", "Job updated", "job", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        try {
            jobService.deleteJob(id);
            return ResponseEntity.ok(Map.of("message", "Job " + id + " deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
