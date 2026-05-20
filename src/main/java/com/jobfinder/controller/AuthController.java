package com.jobfinder.controller;

import com.jobfinder.config.RateLimitConfig;
import com.jobfinder.dto.request.JobSeekerSignupRequest;
import com.jobfinder.dto.request.LoginRequest;
import com.jobfinder.dto.request.RecruiterSignupRequest;
import com.jobfinder.dto.response.AuthResponse;
import com.jobfinder.dto.response.UserResponse;
import com.jobfinder.exception.AppException;
import com.jobfinder.security.JwtService;
import com.jobfinder.service.AuthService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces src/routes/auth.route.ts + src/controllers/auth.controller.ts
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService      authService;
    private final JwtService       jwtService;
    private final RateLimitConfig  rateLimitConfig;

    // POST /api/auth/signup/jobseeker  (rate: 5/hr)
    @PostMapping("/signup/jobseeker")
    public ResponseEntity<?> signupJobSeeker(@Valid @RequestBody JobSeekerSignupRequest req,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        checkSignupRateLimit(request);
        AuthResponse body = authService.signupJobSeeker(req.name(), req.email(), req.password(), response);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // POST /api/auth/signup/recruiter  (rate: 5/hr)
    @PostMapping("/signup/recruiter")
    public ResponseEntity<?> signupRecruiter(@Valid @RequestBody RecruiterSignupRequest req,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        checkSignupRateLimit(request);
        AuthResponse body = authService.signupRecruiter(req, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // POST /api/auth/upgrade/recruiter  (JOB_SEEKER only)
    @PostMapping("/upgrade/recruiter")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public ResponseEntity<?> upgradeToRecruiter(@Valid @RequestBody RecruiterSignupRequest req,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        UUID userId = currentUserId(request);
        AuthResponse body = authService.upgradeToRecruiter(userId, req, response);
        return ResponseEntity.ok(body);
    }

    // POST /api/auth/login  (rate: 10/15min)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        checkLoginRateLimit(request);
        AuthResponse body = authService.login(req.email(), req.password(), response);
        return ResponseEntity.ok(body);
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        jwtService.clearTokenCookies(response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // POST /api/auth/refresh  (rate: 10/15min)
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request,
                                      HttpServletResponse response) {
        checkLoginRateLimit(request);
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "No refresh token provided"));
        }
        AuthResponse body = authService.refreshTokens(refreshToken, response);
        return ResponseEntity.ok(body);
    }

    // GET /api/auth/me  (authenticated)
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpServletRequest request) {
        UUID userId = currentUserId(request);
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private void checkSignupRateLimit(HttpServletRequest request) {
        Bucket bucket = rateLimitConfig.resolveSignupBucket(clientIp(request));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new AppException("Too many signup attempts. Try again later.",
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private void checkLoginRateLimit(HttpServletRequest request) {
        Bucket bucket = rateLimitConfig.resolveLoginBucket(clientIp(request));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new AppException("Too many requests. Try again later.",
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    private UUID currentUserId(HttpServletRequest request) {
        // Principal is the userId string set by CookieAuthFilter
        return UUID.fromString(
            org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName()
        );
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
            .filter(c -> name.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst().orElse(null);
    }
}
