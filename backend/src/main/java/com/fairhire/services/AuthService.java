package com.fairhire.services;

import com.fairhire.config.JwtUtils;
import com.fairhire.models.User;
import com.fairhire.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    public Map<String, Object> register(String name, String email, String password, String role) {
        String cleanEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(name.trim(), cleanEmail, encodedPassword, (role != null ? role.trim() : "RECRUITER"));
        userRepository.save(user);

        String roleStr = user.getRole() != null ? user.getRole().name() : "RECRUITER";
        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), roleStr);
        return Map.of(
                "user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(), "role", roleStr),
                "token", token
        );
    }

    public Map<String, Object> login(String email, String password) {
        String cleanEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        User user = userOpt.get();
        String roleStr = user.getRole() != null ? user.getRole().name() : "RECRUITER";
        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), roleStr);
        return Map.of(
                "user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(), "role", roleStr),
                "token", token
        );
    }

    public Map<String, Object> getMe(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid token format.");
        }
        String jwt = token.substring(7);
        if (!jwtUtils.validateToken(jwt)) {
            throw new IllegalArgumentException("Expired or invalid token.");
        }
        var claims = jwtUtils.parseToken(jwt);
        Long userId = Long.parseLong(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        String roleStr = user.getRole() != null ? user.getRole().name() : "RECRUITER";
        return Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(), "role", roleStr);
    }
}
