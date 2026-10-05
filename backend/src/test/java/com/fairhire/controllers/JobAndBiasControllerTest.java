package com.fairhire.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobAndBiasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, String> credentials = Map.of(
                "email", "recruiter@fairhire.ai",
                "password", "password123"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        jwtToken = (String) response.get("token");
    }

    @Test
    void testGetJobsList() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testCreateJobDescription() throws Exception {
        Map<String, Object> jobPayload = Map.of(
                "title", "Senior Full-Stack Engineer",
                "department", "Engineering",
                "description", "Looking for an energetic, rockstar ninja developer skilled in Python, React, and SQL.",
                "skills", List.of("Python", "React", "SQL")
        );

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Senior Full-Stack Engineer"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void testBiasAnalysisDirect() throws Exception {
        Map<String, String> biasPayload = Map.of(
                "text", "We need a young, aggressive rockstar who can dominate the fast-paced market."
        );

        mockMvc.perform(post("/api/bias/analyze")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(biasPayload)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.error").exists());
    }
}
