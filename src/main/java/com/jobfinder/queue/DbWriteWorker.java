package com.jobfinder.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobfinder.service.JobSeekerService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.Executor;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Replaces the Bull worker in worker.ts from the Node.js backend.
 *
 * Uses BRPOP (blocking right-pop) on the Redis LIST, which is equivalent
 * to Bull's process() callback model. Each iteration of the polling loop
 * blocks for up to 2 seconds waiting for a job, then retries.
 *
 * Concurrency is handled by the dedicated "queueWorkerExecutor" thread pool
 * defined in AsyncConfig — N threads each running their own BRPOP loop,
 * matching Bull's { concurrency: N } option.
 *
 * Retry logic: up to 3 attempts with exponential backoff (500ms base),
 * matching Bull's defaultJobOptions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DbWriteWorker {

    private static final int    MAX_ATTEMPTS  = 3;
    private static final long   BACKOFF_MS    = 500;
    private static final int    BRPOP_TIMEOUT = 2; // seconds

    private final RedisTemplate<String, String> redisTemplate;
    private final DbWriteQueueService           queueService;
    private final JobSeekerService              jobSeekerService;
    private final ObjectMapper                  objectMapper;

    @Value("${app.queue.concurrency:5}")
    private int concurrency;

    @Autowired
    @Qualifier("queueWorkerExecutor")
    private Executor queueWorkerExecutor;

    /**
     * Starts N concurrent worker loops on the dedicated thread pool.
     * Called automatically after the Spring context is ready.
     */
    @PostConstruct
    public void startWorkers() {
        for (int i = 0; i < concurrency; i++) {
            queueWorkerExecutor.execute(this::runWorkerLoop);
        }
        log.info("[Worker] db-write-queue worker started (concurrency: {})", concurrency);
    }

    /**
     * Single polling loop — runs indefinitely on the queueWorkerExecutor thread pool.
     * Equivalent to Bull's dbWriteQueue.process(CONCURRENCY, handler).
     */
    public void runWorkerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Blocking right-pop: waits up to BRPOP_TIMEOUT seconds for a job
                String jobId = redisTemplate.opsForList()
                    .rightPop(DbWriteQueueService.QUEUE_LIST_KEY, BRPOP_TIMEOUT, TimeUnit.SECONDS);

                if (jobId == null) continue; // timeout — loop again

                processWithRetry(jobId);

            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    log.info("[Worker] Worker thread interrupted, shutting down");
                    break;
                }
                log.error("[Worker] Unexpected error in worker loop", e);
                // Brief pause before retrying to avoid busy-loop on persistent errors
                try { Thread.sleep(1000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // -------------------------------------------------------------------
    // processWithRetry
    // -------------------------------------------------------------------

    private void processWithRetry(String jobId) {
        String type = queueService.getType(jobId);
        log.debug("[Worker] Processing job #{} type={}", jobId, type);
        queueService.setStatus(jobId, "active");

        int attempt = 0;
        while (attempt < MAX_ATTEMPTS) {
            attempt++;
            queueService.incrementAttempts(jobId);
            try {
                Object result = dispatch(jobId, type);
                queueService.setResult(jobId, result);
                queueService.setStatus(jobId, "completed");
                log.info("[Worker] ✓ Job #{} ({}) completed on attempt {}", jobId, type, attempt);
                return;
            } catch (Exception e) {
                log.warn("[Worker] ✗ Job #{} ({}) failed attempt {}/{}: {}",
                    jobId, type, attempt, MAX_ATTEMPTS, e.getMessage());

                if (attempt >= MAX_ATTEMPTS) {
                    queueService.setFailedReason(jobId, e.getMessage());
                    queueService.setStatus(jobId, "failed");
                    log.error("[Worker] ✗ Job #{} ({}) permanently failed after {} attempts",
                        jobId, type, MAX_ATTEMPTS);
                } else {
                    // Exponential backoff: 500ms, 1000ms, 2000ms ...
                    long delay = BACKOFF_MS * (1L << (attempt - 1));
                    try { Thread.sleep(delay); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------
    // dispatch — equivalent to Bull's switch(data.type)
    // -------------------------------------------------------------------

    private Object dispatch(String jobId, String type) throws Exception {
        String payloadJson = queueService.getPayloadJson(jobId);
        return switch (type) {
            case "apply-to-job" -> {
                ApplyToJobPayload p = objectMapper.readValue(payloadJson, ApplyToJobPayload.class);
                yield jobSeekerService.applyToJob(
                    UUID.fromString(p.getUserId()),
                    UUID.fromString(p.getJobId()),
                    p.getCoverLetter()
                );
            }
            case "save-job" -> {
                SaveJobPayload p = objectMapper.readValue(payloadJson, SaveJobPayload.class);
                yield jobSeekerService.saveJob(
                    UUID.fromString(p.getUserId()),
                    UUID.fromString(p.getJobId())
                );
            }
            default -> throw new IllegalArgumentException("Unknown job type: " + type);
        };
    }
}
