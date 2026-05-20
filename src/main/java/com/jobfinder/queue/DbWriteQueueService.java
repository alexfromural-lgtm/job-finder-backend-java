package com.jobfinder.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobfinder.dto.response.QueueJobStatusResponse;
import com.jobfinder.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
@Service
@RequiredArgsConstructor
@Slf4j
public class DbWriteQueueService {

    static final String QUEUE_LIST_KEY   = "db-write-queue";
    static final String META_KEY_PREFIX  = "queue-meta:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper                  objectMapper;

    // -------------------------------------------------------------------
    // enqueue
    // -------------------------------------------------------------------

    /**
     * Serialize the payload, store metadata in a Redis hash, then LPUSH
     * the jobId onto the queue LIST so the worker can BRPOP it.
     */
    public String enqueue(QueueJobPayload payload) {
        String jobId = UUID.randomUUID().toString();
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String metaKey = META_KEY_PREFIX + jobId;

            // Store metadata hash
            redisTemplate.opsForHash().putAll(metaKey, Map.of(
                "id",           jobId,
                "type",         payload.getType(),
                "status",       "waiting",
                "attemptsMade", "0",
                "createdAt",    Instant.now().toString(),
                "payload",      payloadJson
            ));

            // Push jobId onto the queue list for the worker to consume
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

    public QueueJobStatusResponse getJobStatus(String jobId) {
        String metaKey = META_KEY_PREFIX + jobId;
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(metaKey);

        if (fields == null || fields.isEmpty()) {
            throw new ResourceNotFoundException("Queue job not found: " + jobId);
        }

        String createdAtStr = (String) fields.getOrDefault("createdAt", Instant.now().toString());
        String resultJson   = (String) fields.get("result");
        Object result       = null;
        if (resultJson != null) {
            try { result = objectMapper.readValue(resultJson, Object.class); }
            catch (Exception ignored) { result = resultJson; }
        }

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

    void setStatus(String jobId, String status) {
        redisTemplate.opsForHash().put(META_KEY_PREFIX + jobId, "status", status);
    }

    void incrementAttempts(String jobId) {
        redisTemplate.opsForHash().increment(META_KEY_PREFIX + jobId, "attemptsMade", 1);
    }

    void setResult(String jobId, Object result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForHash().put(META_KEY_PREFIX + jobId, "result", json);
        } catch (Exception e) {
            log.warn("[Queue] Could not serialize result for job #{}", jobId);
        }
    }

    void setFailedReason(String jobId, String reason) {
        redisTemplate.opsForHash().put(META_KEY_PREFIX + jobId, "failedReason", reason);
    }

    String getPayloadJson(String jobId) {
        return (String) redisTemplate.opsForHash().get(META_KEY_PREFIX + jobId, "payload");
    }

    String getType(String jobId) {
        return (String) redisTemplate.opsForHash().get(META_KEY_PREFIX + jobId, "type");
    }
}
