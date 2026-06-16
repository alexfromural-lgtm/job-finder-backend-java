package com.jobfinder.security;

// Import JWT claims representation class
import io.jsonwebtoken.Claims;
// Java Servlet classes to handle HTTP request filtering and filter chains
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// Lombok annotations to auto-generate constructor and logger injection
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Spring Framework annotations and classes for security configuration, filters, and null-safety
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Standard Java collections and IO/stream utility classes
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Replaces auth.middleware.ts (requireAuth) from the Node.js backend.
 *
 * Reads the 'accessToken' cookie from each incoming request.
 * If valid, populates the Spring SecurityContext so downstream
 * Spring Security rules and @PreAuthorize annotations work correctly.
 *
 * On failure the filter passes through — the SecurityFilterChain will
 * return 401 for any protected endpoint that requires authentication.
 */
// Registers this class as a Spring Component bean
@Component
// Generates a constructor injecting final dependencies
@RequiredArgsConstructor
// Generates a logger named 'log'
@Slf4j
public class CookieAuthFilter extends OncePerRequestFilter {

    // Inject the JwtService containing token validation helper methods
    private final JwtService jwtService;

    // Filters every incoming HTTP request exactly once
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        // Extract the accessToken value from the request cookies
        String token = extractCookie(request, "accessToken");

        // Process token if present and if user is not already authenticated in this thread context
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Verify the JWT access token and retrieve its claims payload
                Claims claims = jwtService.verifyAccessToken(token);

                // Subject of the JWT is the unique user ID string
                String userId = claims.getSubject();
                // Read the roles list claim from the JWT
                List<?> rawRoles = claims.get("roles", List.class);
                // Map roles to Spring GrantedAuthority instances prefixed with "ROLE_"
                List<SimpleGrantedAuthority> authorities = rawRoles == null
                    ? Collections.emptyList()
                    : rawRoles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toString()))
                        .toList();

                // Create authentication token containing user ID principal and authorities list
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                // Bind remote IP and session context details to authentication token
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Set authentication token on SecurityContextHolder to declare request authenticated
                SecurityContextHolder.getContext().setAuthentication(authToken);

            } catch (Exception e) {
                // Invalid / expired token — pass through, endpoint security handles 401
                log.debug("Cookie auth failed: {}", e.getMessage());
            }
        }

        // Pass the request along the filter chain to next filters or target controller
        chain.doFilter(request, response);
    }

    // Helper method to scan request cookies and fetch a specific cookie's value by name
    private String extractCookie(HttpServletRequest request, String name) {
        // Returns null if no cookies exist in the request header
        if (request.getCookies() == null) return null;
        // Stream search cookies for target name match and return value
        return Arrays.stream(request.getCookies())
            .filter(c -> name.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
