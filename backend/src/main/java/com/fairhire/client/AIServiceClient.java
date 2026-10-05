package com.fairhire.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class AIServiceClient {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public AIServiceClient(RestTemplateBuilder builder, @Value("${ai.service.url:http://localhost:5000}") String aiServiceUrl) {
        this.restTemplate = builder.build();
        this.aiServiceUrl = aiServiceUrl;
    }

    /**
     * Call Python AI microservice to analyze JD for bias and readability.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeBias(String text) {
        try {
            String endpoint = aiServiceUrl + "/api/bias/analyze";
            Map<String, String> request = Map.of("text", text);
            return restTemplate.postForObject(endpoint, request, Map.class);
        } catch (Exception e) {
            System.err.println("[AIServiceClient] AI service unavailable for analyzeBias: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "AI microservice unavailable: " + e.getMessage());
            errorResponse.put("status", "SERVICE_UNAVAILABLE");
            errorResponse.put("service", "fairhire-ai-service");
            return errorResponse;
        }
    }

    /**
     * Call Python AI microservice to generate an inclusive neutral rewrite.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> rewriteInclusive(String text) {
        try {
            String endpoint = aiServiceUrl + "/api/bias/rewrite";
            Map<String, String> request = Map.of("text", text);
            return restTemplate.postForObject(endpoint, request, Map.class);
        } catch (Exception e) {
            System.err.println("[AIServiceClient] AI service unavailable for rewriteInclusive: " + e.getMessage());
            return Map.of(
                    "error", "AI microservice unavailable: " + e.getMessage(),
                    "status", "SERVICE_UNAVAILABLE",
                    "service", "fairhire-ai-service"
            );
        }
    }

    /**
     * Compute semantic matching score using Sentence-BERT embeddings.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> computeSemanticScore(String jdText, String resumeText) {
        try {
            String endpoint = aiServiceUrl + "/api/matching/score";
            Map<String, String> request = Map.of("jd_text", jdText, "resume_text", resumeText);
            return restTemplate.postForObject(endpoint, request, Map.class);
        } catch (Exception e) {
            System.err.println("[AIServiceClient] AI service unavailable for computeSemanticScore: " + e.getMessage());
            return Map.of(
                    "error", "AI microservice unavailable: " + e.getMessage(),
                    "status", "SERVICE_UNAVAILABLE",
                    "service", "fairhire-ai-service"
            );
        }
    }
}

