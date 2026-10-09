package com.fairhire.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Filter that validates Bearer JWT tokens on incoming requests.
 * Establishes Spring Security authentication before domain-level authorization.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (jwtUtils.validateToken(token)) {
                try {
                    Claims claims = jwtUtils.parseToken(token);
                    String userId = claims.getSubject();
                    String email = claims.get("email", String.class);
                    String role = claims.get("role", String.class);
                    String department = claims.get("department", String.class);

                    Long parsedUserId = null;
                    if (userId != null) {
                        try {
                            parsedUserId = Long.parseLong(userId);
                        } catch (NumberFormatException ignored) {}
                    }

                    FairHireUserPrincipal principal = new FairHireUserPrincipal(
                            parsedUserId,
                            email,
                            role,
                            department != null && !department.isBlank() ? department.trim() : null
                    );

                    List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            : Collections.emptyList();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    SecurityContextHolder.clearContext();
                }
            } else {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
