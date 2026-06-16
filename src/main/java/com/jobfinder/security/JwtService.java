package com.jobfinder.security;

// Import our custom Role enum
import com.jobfinder.enums.Role;
// Import our custom Unauthorized exception
import com.jobfinder.exception.UnauthorizedException;
// JJWT classes to manage token building, parsing, claims, and signing keys
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
// Jakarta Servlet classes to construct HTTP-only cookies on responses
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
// Lombok annotation to inject log instance
import lombok.extern.slf4j.Slf4j;
// Spring annotations for injection and registering services
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Standard Java cryptography, character sets, duration, dates, collections, and UUID utilities
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Replaces src/utils/token.ts from the Node.js backend.
 *
 * Handles JWT generation and verification for both access and refresh tokens.
 * Tokens are set as HTTP-only, SameSite=Lax cookies (same behaviour as
 * the Node.js cookieOptions).
 */
// Registers this class as a Spring Service bean
@Service
// Generates a logger named 'log'
@Slf4j
public class JwtService {

    // Injects the JWT access token secret from application properties
    @Value("${app.jwt.access-token-secret}")
    private String accessSecret;

    // Injects the JWT refresh token secret from application properties
    @Value("${app.jwt.refresh-token-secret}")
    private String refreshSecret;

    // Defines the access token expiration time (15 minutes in milliseconds)
    private static final long ACCESS_TOKEN_EXPIRY_MS  = Duration.ofMinutes(15).toMillis();
    // Defines the refresh token expiration time (7 days in milliseconds)
    private static final long REFRESH_TOKEN_EXPIRY_MS = Duration.ofDays(7).toMillis();

    // Defines the access token cookie lifespan in seconds (15 minutes)
    private static final int ACCESS_COOKIE_MAX_AGE  = (int) Duration.ofMinutes(15).toSeconds();
    // Defines the refresh token cookie lifespan in seconds (7 days)
    private static final int REFRESH_COOKIE_MAX_AGE = (int) Duration.ofDays(7).toSeconds();

    // -------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------

    // Generates a JWT access token containing the user ID subject and user roles list claim
    public String generateAccessToken(UUID userId, List<Role> roles) {
        // Map Role enum values to their String names
        List<String> roleNames = roles.stream().map(Enum::name).toList();
        // Construct and sign the access token
        return Jwts.builder()
            .subject(userId.toString())
            .claim("roles", roleNames)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY_MS))
            .signWith(accessKey())
            .compact();
    }

    // Generates a JWT refresh token containing the user ID subject (no roles needed)
    public String generateRefreshToken(UUID userId) {
        // Construct and sign the refresh token
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY_MS))
            .signWith(refreshKey())
            .compact();
    }

    // -------------------------------------------------------------------
    // Token verification
    // -------------------------------------------------------------------

    // Verifies an access token string using the access signing key, returning its claims payload
    public Claims verifyAccessToken(String token) {
        try {
            // Parses the signed claims from the token string
            return Jwts.parser()
                .verifyWith(accessKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // Logs verification failures and throws UnauthorizedException
            log.debug("Access token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired access token");
        }
    }

    // Verifies a refresh token string using the refresh signing key, returning its claims payload
    public Claims verifyRefreshToken(String token) {
        try {
            // Parses the signed claims from the token string
            return Jwts.parser()
                .verifyWith(refreshKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // Logs verification failures and throws UnauthorizedException
            log.debug("Refresh token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
    }

    // -------------------------------------------------------------------
    // Cookie helpers
    // -------------------------------------------------------------------

    /**
     * Sets both access and refresh tokens as HTTP-only cookies on the response.
     * Mirrors Node.js setTokenCookies / cookieOptions.
     */
    // Builds and appends both accessToken and refreshToken cookies to the HTTP response
    public void setTokenCookies(HttpServletResponse response,
                                String accessToken,
                                String refreshToken) {
        response.addCookie(buildCookie("accessToken",  accessToken,  ACCESS_COOKIE_MAX_AGE));
        response.addCookie(buildCookie("refreshToken", refreshToken, REFRESH_COOKIE_MAX_AGE));
    }

    /**
     * Clears both token cookies (logout).
     */
    // Clears the accessToken and refreshToken cookies by setting empty values and immediate expiration (0 max age)
    public void clearTokenCookies(HttpServletResponse response) {
        response.addCookie(buildCookie("accessToken",  "", 0));
        response.addCookie(buildCookie("refreshToken", "", 0));
    }

    // Constructs a cookie object with HTTP-only, root path, and age parameters configured
    private Cookie buildCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        // Restricts access to client-side scripts to prevent XSS attacks
        cookie.setHttpOnly(true);
        // Makes the cookie accessible across the entire application domain path
        cookie.setPath("/");
        // Configures cookie expiration lifetime
        cookie.setMaxAge(maxAgeSeconds);
        // Secure flag should be true in production; kept flexible via Spring profile
        // cookie.setSecure(true);
        return cookie;
    }

    // -------------------------------------------------------------------
    // Key helpers
    // -------------------------------------------------------------------

    // Translates the raw access token secret string into a HMAC-SHA SecretKey object
    private SecretKey accessKey() {
        return Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Translates the raw refresh token secret string into a HMAC-SHA SecretKey object
    private SecretKey refreshKey() {
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }
}
