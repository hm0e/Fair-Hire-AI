package com.fairhire.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class HealthController {

    private final DataSource dataSource;
    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public HealthController(DataSource dataSource,
                            RestTemplateBuilder restTemplateBuilder,
                            @Value("${ai.service.url:http://localhost:5000}") String aiServiceUrl) {
        this.dataSource = dataSource;
        this.restTemplate = restTemplateBuilder.build();
        this.aiServiceUrl = aiServiceUrl;
    }

    @GetMapping({"/api/health", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "fairhire-backend");
        response.put("version", "1.0.0");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/health/system")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> systemReport = new HashMap<>();
        systemReport.put("service", "fairhire-backend");
        systemReport.put("timestamp", Instant.now().toString());

        // 1. Check Database connectivity
        Map<String, Object> dbCheck = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            String dbProduct = connection.getMetaData().getDatabaseProductName();
            String dbVersion = connection.getMetaData().getDatabaseProductVersion();
            dbCheck.put("status", valid ? "UP" : "DOWN");
            dbCheck.put("database", dbProduct);
            dbCheck.put("version", dbVersion);
        } catch (Exception e) {
            dbCheck.put("status", "DOWN");
            dbCheck.put("error", e.getMessage());
        }
        systemReport.put("database", dbCheck);

        // 2. Check AI Microservice connectivity
        Map<String, Object> aiCheck = new HashMap<>();
        try {
            String target = aiServiceUrl + "/api/v1/health";
            @SuppressWarnings("unchecked")
            Map<String, Object> aiResponse = restTemplate.getForObject(target, Map.class);
            aiCheck.put("status", "UP");
            aiCheck.put("details", aiResponse);
        } catch (Exception e) {
            aiCheck.put("status", "DOWN");
            aiCheck.put("error", e.getMessage());
        }
        systemReport.put("ai_service", aiCheck);

        // Overall status
        boolean dbOk = "UP".equals(dbCheck.get("status"));
        boolean aiOk = "UP".equals(aiCheck.get("status"));
        if (dbOk && aiOk) {
            systemReport.put("status", "UP");
        } else if (dbOk) {
            systemReport.put("status", "DEGRADED");
        } else {
            systemReport.put("status", "DOWN");
        }

        return ResponseEntity.ok(systemReport);
    }
}
