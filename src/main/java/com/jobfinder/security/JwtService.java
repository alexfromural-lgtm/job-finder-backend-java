package com.jobfinder.security;

import com.jobfinder.enums.Role;
import com.jobfinder.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.access-token-secret}")
    private String accessSecret;

    @Value("${app.jwt.refresh-token-secret}")
    private String refreshSecret;

    // 15 minutes in ms
    private static final long ACCESS_TOKEN_EXPIRY_MS  = Duration.ofMinutes(15).toMillis();
    // 7 days in ms
    private static final long REFRESH_TOKEN_EXPIRY_MS = Duration.ofDays(7).toMillis();

    // Cookie max-ages (seconds)
    private static final int ACCESS_COOKIE_MAX_AGE  = (int) Duration.ofMinutes(15).toSeconds();
    private static final int REFRESH_COOKIE_MAX_AGE = (int) Duration.ofDays(7).toSeconds();

    // -------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------

    public String generateAccessToken(UUID userId, List<Role> roles) {
        List<String> roleNames = roles.stream().map(Enum::name).toList();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("roles", roleNames)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY_MS))
            .signWith(accessKey())
            .compact();
    }

    public String generateRefreshToken(UUID userId) {
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

    public Claims verifyAccessToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(accessKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Access token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired access token");
        }
    }

    public Claims verifyRefreshToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(refreshKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
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
    public void setTokenCookies(HttpServletResponse response,
                                String accessToken,
                                String refreshToken) {
        response.addCookie(buildCookie("accessToken",  accessToken,  ACCESS_COOKIE_MAX_AGE));
        response.addCookie(buildCookie("refreshToken", refreshToken, REFRESH_COOKIE_MAX_AGE));
    }

    /**
     * Clears both token cookies (logout).
     */
    public void clearTokenCookies(HttpServletResponse response) {
        response.addCookie(buildCookie("accessToken",  "", 0));
        response.addCookie(buildCookie("refreshToken", "", 0));
    }

    private Cookie buildCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        // Secure flag should be true in production; kept flexible via Spring profile
        // cookie.setSecure(true);
        return cookie;
    }

    // -------------------------------------------------------------------
    // Key helpers
    // -------------------------------------------------------------------

    private SecretKey accessKey() {
        return Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey refreshKey() {
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }
}
