package com.fairhire.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRecruiterLoginSuccess() throws Exception {
        Map<String, String> credentials = Map.of(
                "email", "recruiter@fairhire.ai",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value("recruiter@fairhire.ai"))
                .andExpect(jsonPath("$.user.role").value("RECRUITER"));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        Map<String, String> badCredentials = Map.of(
                "email", "recruiter@fairhire.ai",
                "password", "wrongpassword"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badCredentials)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegisterNewUser() throws Exception {
        String testEmail = "testuser_" + System.currentTimeMillis() + "@fairhire.ai";
        Map<String, String> newUser = Map.of(
                "email", testEmail,
                "password", "secretpass123",
                "name", "Test User",
                "role", "RECRUITER"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(testEmail));
    }
}
