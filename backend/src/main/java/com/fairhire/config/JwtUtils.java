package com.fairhire.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${jwt.secret:fairhire-ai-secret-key-2026-research-edition-spring-boot-jwt-token-key-32chars}")
    private String jwtSecret;

    @Value("${jwt.expiration-hours:24}")
    private int expirationHours;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String email, String role) {
        return generateToken(userId, email, role, null);
    }

    public String generateToken(Long userId, String email, String role, String department) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (long) expirationHours * 3600 * 1000);

        Map<String, Object> claims = new java.util.HashMap<>();
        if (email != null) claims.put("email", email);
        if (role != null) claims.put("role", role);
        if (department != null && !department.isBlank()) {
            claims.put("department", department.trim());
        }

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateExpiredToken(Long userId, String email, String role) {
        return generateExpiredToken(userId, email, role, null);
    }

    public String generateExpiredToken(Long userId, String email, String role, String department) {
        Date now = new Date(System.currentTimeMillis() - 7200000);
        Date expiryDate = new Date(System.currentTimeMillis() - 3600000);

        Map<String, Object> claims = new java.util.HashMap<>();
        if (email != null) claims.put("email", email);
        if (role != null) claims.put("role", role);
        if (department != null && !department.isBlank()) {
            claims.put("department", department.trim());
        }

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
