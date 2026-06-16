package com.jobfinder.controller;

// Import our custom rate limiting configurations
import com.jobfinder.config.RateLimitConfig;
// Import our payload DTO classes
import com.jobfinder.dto.request.JobSeekerSignupRequest;
import com.jobfinder.dto.request.LoginRequest;
import com.jobfinder.dto.request.RecruiterSignupRequest;
// Import our response DTO classes
import com.jobfinder.dto.response.AuthResponse;
import com.jobfinder.dto.response.UserResponse;
// Import custom app exception shape
import com.jobfinder.exception.AppException;
// Import stateless JWT utility service
import com.jobfinder.security.JwtService;
// Import core AuthService executing database credentials operations
import com.jobfinder.service.AuthService;
// Bucket4j rate limiting component classes
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
// Jakarta servlet API components
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// Spring validation and security annotations
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Standard Java collections and unique identifier utilities
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces src/routes/auth.route.ts + src/controllers/auth.controller.ts
 */
// Marks this class as a REST Controller handling HTTP requests
@RestController
// Maps the base request path URI for all endpoints in this controller
@RequestMapping("/api/auth")
// Generates standard constructor injecting dependencies automatically
@RequiredArgsConstructor
public class AuthController {

    // Inject AuthService bean
    private final AuthService      authService;
    // Inject JwtService bean
    private final JwtService       jwtService;
    // Inject RateLimitConfig bean
    private final RateLimitConfig  rateLimitConfig;

    // POST /api/auth/signup/jobseeker  (rate: 5/hr)
    // Submits new candidate signups, subject to custom signup rate limits
    @PostMapping("/signup/jobseeker")
    public ResponseEntity<?> signupJobSeeker(@Valid @RequestBody JobSeekerSignupRequest req,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        // Enforce rate limiter rules against client IP address
        checkSignupRateLimit(request);
        // Execute candidate sign up in the database and issue JWT cookies on the response
        AuthResponse body = authService.signupJobSeeker(req.name(), req.email(), req.password(), response);
        // Return 201 Created with the AuthResponse body
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // POST /api/auth/signup/recruiter  (rate: 5/hr)
    // Submits new recruiter signups, subject to custom signup rate limits
    @PostMapping("/signup/recruiter")
    public ResponseEntity<?> signupRecruiter(@Valid @RequestBody RecruiterSignupRequest req,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        // Enforce rate limiter rules against client IP address
        checkSignupRateLimit(request);
        // Execute recruiter sign up in the database and issue JWT cookies on the response
        AuthResponse body = authService.signupRecruiter(req, response);
        // Return 201 Created with the AuthResponse body
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // POST /api/auth/upgrade/recruiter  (JOB_SEEKER only)
    // Upgrades candidate profile to recruiter profile
    @PostMapping("/upgrade/recruiter")
    // Restricts access to users holding the JOB_SEEKER role prior to execution
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public ResponseEntity<?> upgradeToRecruiter(@Valid @RequestBody RecruiterSignupRequest req,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        // Extract authenticated user ID from context
        UUID userId = currentUserId(request);
        // Upgrade user roles and attach recruiter company metadata to profile
        AuthResponse body = authService.upgradeToRecruiter(userId, req, response);
        // Return 200 OK with AuthResponse
        return ResponseEntity.ok(body);
    }

    // POST /api/auth/login  (rate: 10/15min)
    // Authenticates credentials and issues session cookie tokens
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        // Enforce rate limiter rules against client IP address
        checkLoginRateLimit(request);
        // Verify credentials and attach session cookies on the HTTP response headers
        AuthResponse body = authService.login(req.email(), req.password(), response);
        // Return 200 OK with AuthResponse
        return ResponseEntity.ok(body);
    }

    // POST /api/auth/logout
    // Invalidates session cookies to log out the user
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Clear both accessToken and refreshToken cookies by expiring them immediately
        jwtService.clearTokenCookies(response);
        // Return 200 OK with success confirmation message
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // POST /api/auth/refresh  (rate: 10/15min)
    // Refreshes the access token using the refresh token cookie
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request,
                                      HttpServletResponse response) {
        // Enforce rate limiter rules against client IP address
        checkLoginRateLimit(request);
        // Extract the raw refresh token string from the request cookie store
        String refreshToken = extractCookie(request, "refreshToken");
        // Throw 401 Unauthorized if cookie is missing
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "No refresh token provided"));
        }
        // Verify refresh token signature and issue updated cookie credentials
        AuthResponse body = authService.refreshTokens(refreshToken, response);
        // Return 200 OK with AuthResponse details
        return ResponseEntity.ok(body);
    }

    // GET /api/auth/me  (authenticated)
    // Returns details of the currently authenticated session user
    @GetMapping("/me")
    public ResponseEntity<Map<String, UserResponse>> me(HttpServletRequest request) {
        // Extract current authenticated user ID from security context
        UUID userId = currentUserId(request);
        // Query database and return mapped UserResponse
        return ResponseEntity.ok(Map.of("user", authService.getCurrentUser(userId)));
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    // Validates signup rate limits for the given client IP address
    private void checkSignupRateLimit(HttpServletRequest request) {
        Bucket bucket = rateLimitConfig.resolveSignupBucket(clientIp(request));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        // Throw 429 TOO_MANY_REQUESTS exception if signup limit is exceeded
        if (!probe.isConsumed()) {
            throw new AppException("Too many signup attempts. Try again later.",
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    // Validates login rate limits for the given client IP address
    private void checkLoginRateLimit(HttpServletRequest request) {
        Bucket bucket = rateLimitConfig.resolveLoginBucket(clientIp(request));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        // Throw 429 TOO_MANY_REQUESTS exception if login limit is exceeded
        if (!probe.isConsumed()) {
            throw new AppException("Too many requests. Try again later.",
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    // Resolves raw client IP address, checking X-Forwarded-For headers when proxied
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    // Resolves authenticated UUID string from current Spring Security context
    private UUID currentUserId(HttpServletRequest request) {
        // Principal is the userId string set by CookieAuthFilter
        return UUID.fromString(
            org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName()
        );
    }

    // Extraction helper searching HTTP request cookies by name parameter
    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
            .filter(c -> name.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst().orElse(null);
    }
}
