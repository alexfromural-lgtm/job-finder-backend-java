package com.jobfinder.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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
@Component
@RequiredArgsConstructor
@Slf4j
public class CookieAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String token = extractCookie(request, "accessToken");

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.verifyAccessToken(token);

                String userId = claims.getSubject();
                List<?> rawRoles = claims.get("roles", List.class);
                List<SimpleGrantedAuthority> authorities = rawRoles == null
                    ? Collections.emptyList()
                    : rawRoles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toString()))
                        .toList();

                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

            } catch (Exception e) {
                // Invalid / expired token — pass through, endpoint security handles 401
                log.debug("Cookie auth failed: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
            .filter(c -> name.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
