package com.jobfinder.queue;

// Jackson class mapping payloads to JSON representation
import com.fasterxml.jackson.databind.ObjectMapper;
// Import the response DTO mapping background jobs status
import com.jobfinder.dto.response.QueueJobStatusResponse;
// Import custom exception mapping resource missing scenarios
import com.jobfinder.exception.ResourceNotFoundException;
// Lombok annotations for constructor injection and loggers
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Spring Redis template dependency for data access
import org.springframework.data.redis.core.RedisTemplate;
// Spring service registration annotation
import org.springframework.stereotype.Service;

// Standard Java dates, maps, and unique identifiers
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces the Bull queue setup in queue.ts from the Node.js backend.
 *
 * Design:
 *   - Queue LIST key:      "db-write-queue"        (LPUSH to enqueue, BRPOP to dequeue)
 *   - Job metadata key:    "queue-meta:{jobId}"    (Redis hash, all string fields)
 *
 * This mirrors Bull's Redis data structures closely enough that the
 * frontend polling endpoint returns the same response shape.
 */
// Registers this class as a Spring Service bean
@Service
// Generates constructor injecting dependencies automatically
@RequiredArgsConstructor
// Generates log helper field
@Slf4j
public class DbWriteQueueService {

    // Redis list key name representing the background queue queue list
    static final String QUEUE_LIST_KEY   = "db-write-queue";
    // Prefix for the Redis hash keys containing job metadata values
    static final String META_KEY_PREFIX  = "queue-meta:";

    // Inject RedisTemplate bean
    private final RedisTemplate<String, String> redisTemplate;
    // Inject Jackson ObjectMapper bean for JSON parsing
    private final ObjectMapper                  objectMapper;

    // -------------------------------------------------------------------
    // enqueue
    // -------------------------------------------------------------------

    /**
     * Serialize the payload, store metadata in a Redis hash, then LPUSH
     * the jobId onto the queue LIST so the worker can BRPOP it.
     */
    // Pushes a new database write task to the Redis queue and returns its unique job ID
    public String enqueue(QueueJobPayload payload) {
        String jobId = UUID.randomUUID().toString();
        try {
            // Serialize payload to JSON string
            String payloadJson = objectMapper.writeValueAsString(payload);
            // Construct the Redis key for the job metadata hash
            String metaKey = META_KEY_PREFIX + jobId;

            // Store metadata hash containing id, type, status, attemptsMade, createdAt, and payload fields
            redisTemplate.opsForHash().putAll(metaKey, Map.of(
                "id",           jobId,
                "type",         payload.getType(),
                "status",       "waiting",
                "attemptsMade", "0",
                "createdAt",    Instant.now().toString(),
                "payload",      payloadJson
            ));

            // Push jobId onto the queue list for the worker to consume (Left push)
            redisTemplate.opsForList().leftPush(QUEUE_LIST_KEY, jobId);

            log.debug("[Queue] Enqueued job #{} type={}", jobId, payload.getType());
        } catch (Exception e) {
            log.error("[Queue] Failed to enqueue job: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to enqueue job", e);
        }
        return jobId;
    }

    // -------------------------------------------------------------------
    // getJobStatus
    // -------------------------------------------------------------------

    // Queries job status details from the Redis hash store
    public QueueJobStatusResponse getJobStatus(String jobId) {
        String metaKey = META_KEY_PREFIX + jobId;
        // Retrieve all fields from the Redis hash mapped by key
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(metaKey);

        // Throw ResourceNotFoundException if no metadata is found for the given job ID
        if (fields == null || fields.isEmpty()) {
            throw new ResourceNotFoundException("Queue job not found: " + jobId);
        }

        // Parse creation time
        String createdAtStr = (String) fields.getOrDefault("createdAt", Instant.now().toString());
        // Parse raw result JSON and map it to an Object type
        String resultJson   = (String) fields.get("result");
        Object result       = null;
        if (resultJson != null) {
            try { result = objectMapper.readValue(resultJson, Object.class); }
            catch (Exception ignored) { result = resultJson; }
        }

        // Return status response DTO mapping properties
        return new QueueJobStatusResponse(
            jobId,
            (String) fields.getOrDefault("type",         "unknown"),
            (String) fields.getOrDefault("status",       "waiting"),
            Integer.parseInt((String) fields.getOrDefault("attemptsMade", "0")),
            Instant.parse(createdAtStr),
            result,
            (String) fields.get("failedReason")
        );
    }

    // -------------------------------------------------------------------
    // Internal helpers used by DbWriteWorker
    // -------------------------------------------------------------------

    // Updates the job status field (e.g. active, completed, failed) in the Redis metadata hash
    void setStatus(String jobId, String status) {
        redisTemplate.opsForHash().put(META_KEY_PREFIX + jobId, "status", status);
    }

    // Increments the execution attempts counter field in the Redis metadata hash by 1
    void incrementAttempts(String jobId) {
        redisTemplate.opsForHash().increment(META_KEY_PREFIX + jobId, "attemptsMade", 1);
    }

    // Serializes and stores the job execution result object into the Redis metadata hash
    void setResult(String jobId, Object result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForHash().put(META_KEY_PREFIX + jobId, "result", json);
        } catch (Exception e) {
            log.warn("[Queue] Could not serialize result for job #{}", jobId);
        }
    }

    // Stores the failure reason message string into the Redis metadata hash
    void setFailedReason(String jobId, String reason) {
        redisTemplate.opsForHash().put(META_KEY_PREFIX + jobId, "failedReason", reason);
    }

    // Returns the raw payload JSON string from the Redis metadata hash
    String getPayloadJson(String jobId) {
        return (String) redisTemplate.opsForHash().get(META_KEY_PREFIX + jobId, "payload");
    }

    // Returns the type field value from the Redis metadata hash
    String getType(String jobId) {
        return (String) redisTemplate.opsForHash().get(META_KEY_PREFIX + jobId, "type");
    }
}
