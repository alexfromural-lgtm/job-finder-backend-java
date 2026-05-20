package com.jobfinder.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter using Bucket4j token-bucket algorithm.
 *
 * Replaces express-rate-limit from the Node.js backend.
 * Uses one bucket per client IP, stored in a ConcurrentHashMap.
 *
 * Configured limits (matching Node.js config):
 *   - signup:  5 requests / 1 hour
 *   - login:   10 requests / 15 minutes
 */
@Configuration
public class RateLimitConfig {

    // One bucket per IP per limit type
    private final Map<String, Bucket> signupBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> loginBuckets  = new ConcurrentHashMap<>();

    /**
     * Get (or create) the signup rate limit bucket for the given client IP.
     * Limit: 5 requests per hour.
     */
    public Bucket resolveSignupBucket(String clientIp) {
        return signupBuckets.computeIfAbsent(clientIp, key ->
            Bucket.builder()
                .addLimit(Bandwidth.builder()
                    .capacity(5)
                    .refillGreedy(5, Duration.ofHours(1))
                    .build())
                .build()
        );
    }

    /**
     * Get (or create) the login rate limit bucket for the given client IP.
     * Limit: 10 requests per 15 minutes.
     */
    public Bucket resolveLoginBucket(String clientIp) {
        return loginBuckets.computeIfAbsent(clientIp, key ->
            Bucket.builder()
                .addLimit(Bandwidth.builder()
                    .capacity(10)
                    .refillGreedy(10, Duration.ofMinutes(15))
                    .build())
                .build()
        );
    }
}
