package com.jobfinder.config;

// Bucket4j imports for defining and building token-bucket rate limits
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
// Spring configuration annotation to mark this class as a source of beans/configurations
import org.springframework.context.annotation.Configuration;

// Standard Java classes for time durations, maps, and thread-safe concurrent maps
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
// Registers this class as a configuration source in Spring application context
@Configuration
public class RateLimitConfig {

    // Thread-safe map to store rate-limiting buckets for signup requests, keyed by client IP address
    private final Map<String, Bucket> signupBuckets = new ConcurrentHashMap<>();
    // Thread-safe map to store rate-limiting buckets for login requests, keyed by client IP address
    private final Map<String, Bucket> loginBuckets  = new ConcurrentHashMap<>();

    /**
     * Get (or create) the signup rate limit bucket for the given client IP.
     * Limit: 5 requests per hour.
     */
    public Bucket resolveSignupBucket(String clientIp) {
        // Retrieves the existing bucket for this IP from the signup map, or creates a new one atomically if absent
        return signupBuckets.computeIfAbsent(clientIp, key ->
            // Initiates building of a new rate limiting Bucket instance
            Bucket.builder()
                // Adds a bandwidth limit constraint to the bucket
                .addLimit(Bandwidth.builder()
                    // Sets the maximum capacity of tokens the bucket can hold to 5
                    .capacity(5)
                    // Configures the bucket to refill at a rate of 5 tokens every 1 hour, using a greedy strategy
                    .refillGreedy(5, Duration.ofHours(1))
                    // Finalizes the bandwidth limit builder
                    .build())
                // Finalizes the bucket builder
                .build()
        );
    }

    /**
     * Get (or create) the login rate limit bucket for the given client IP.
     * Limit: 10 requests per 15 minutes.
     */
    public Bucket resolveLoginBucket(String clientIp) {
        // Retrieves the existing bucket for this IP from the login map, or creates a new one atomically if absent
        return loginBuckets.computeIfAbsent(clientIp, key ->
            // Initiates building of a new rate limiting Bucket instance
            Bucket.builder()
                // Adds a bandwidth limit constraint to the bucket
                .addLimit(Bandwidth.builder()
                    // Sets the maximum capacity of tokens the bucket can hold to 10
                    .capacity(10)
                    // Configures the bucket to refill at a rate of 10 tokens every 15 minutes, using a greedy strategy
                    .refillGreedy(10, Duration.ofMinutes(15))
                    // Finalizes the bandwidth limit builder
                    .build())
                // Finalizes the bucket builder
                .build()
        );
    }
}
