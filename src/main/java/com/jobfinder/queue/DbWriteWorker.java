package com.jobfinder.queue;

// Jackson mapper to serialize/deserialize payload data
import com.fasterxml.jackson.databind.ObjectMapper;
// Import the service executing job seeker actions (apply, bookmark)
import com.jobfinder.service.JobSeekerService;
// Lifecycle annotations to initialize/teardown background polling threads
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
// Lombok constructor generation and logger injection
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Spring annotations to inject configuration values and qualifiers
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

// Standard Java executor pool classes, time units, and unique identifiers
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
// Registers this class as a Spring Component bean
@Component
// Generates a constructor injecting final fields automatically
@RequiredArgsConstructor
// Generates a logger instance named 'log'
@Slf4j
public class DbWriteWorker {

    // Maximum attempts for executing a queue job before marking it failed
    private static final int    MAX_ATTEMPTS  = 3;
    // Exponential backoff base time delay in milliseconds
    private static final long   BACKOFF_MS    = 500;
    // Timeout in seconds for blocking right-pop (BRPOP) commands
    private static final int    BRPOP_TIMEOUT = 2; // seconds

    // Inject RedisTemplate bean
    private final RedisTemplate<String, String> redisTemplate;
    // Inject DbWriteQueueService bean
    private final DbWriteQueueService           queueService;
    // Inject JobSeekerService bean
    private final JobSeekerService              jobSeekerService;
    // Inject ObjectMapper bean
    private final ObjectMapper                  objectMapper;

    // Shutdown hook flag to stop loop execution
    private volatile boolean active = true;

    // Read the queue worker concurrency count from properties (defaults to 5)
    @Value("${app.queue.concurrency:5}")
    private int concurrency;

    // Inject the custom task executor pool dedicated to queue workers
    @Autowired
    @Qualifier("queueWorkerExecutor")
    private Executor queueWorkerExecutor;

    /**
     * Starts N concurrent worker loops on the dedicated thread pool.
     * Called automatically after the Spring context is ready.
     */
    // Registers post-construction callback to start the worker polling threads
    @PostConstruct
    public void startWorkers() {
        // Run runWorkerLoop concurrently on N threads matching configured concurrency
        for (int i = 0; i < concurrency; i++) {
            queueWorkerExecutor.execute(this::runWorkerLoop);
        }
        log.info("[Worker] db-write-queue worker started (concurrency: {})", concurrency);
    }

    /**
     * Gracefully stops all worker loops when the Spring context shuts down.
     */
    // Registers pre-destruction callback to handle graceful shutdown requests
    @PreDestroy
    public void stopWorkers() {
        active = false;
        log.info("[Worker] db-write-queue worker stopping...");
    }

    /**
     * Single polling loop — runs indefinitely on the queueWorkerExecutor thread pool.
     * Equivalent to Bull's dbWriteQueue.process(CONCURRENCY, handler).
     */
    // Indefinite execution loop waiting for and processing queued jobs
    public void runWorkerLoop() {
        while (active && !Thread.currentThread().isInterrupted()) {
            try {
                // Blocking right-pop: waits up to BRPOP_TIMEOUT seconds for a job ID from the Redis list
                String jobId = redisTemplate.opsForList()
                    .rightPop(DbWriteQueueService.QUEUE_LIST_KEY, BRPOP_TIMEOUT, TimeUnit.SECONDS);

                // If no job ID was popped within the timeout, loop again
                if (jobId == null) continue; // timeout — loop again

                // Execute the job logic with recovery retries
                processWithRetry(jobId);

            } catch (Exception e) {
                // Break loop if the thread is interrupted or stopping
                if (!active || Thread.currentThread().isInterrupted()) {
                    log.info("[Worker] Worker thread stopping/interrupted");
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

    // Processes a queued task with retries and exponential backoff
    private void processWithRetry(String jobId) {
        String type = queueService.getType(jobId);
        log.debug("[Worker] Processing job #{} type={}", jobId, type);
        // Mark job status active in Redis metadata hash
        queueService.setStatus(jobId, "active");

        int attempt = 0;
        // Attempt execution up to MAX_ATTEMPTS times
        while (attempt < MAX_ATTEMPTS) {
            attempt++;
            // Increment the attempts counter in Redis metadata
            queueService.incrementAttempts(jobId);
            try {
                // Dispatch payload parsing and service execution
                Object result = dispatch(jobId, type);
                // Save the result object on successful completion
                queueService.setResult(jobId, result);
                // Mark status completed in Redis metadata
                queueService.setStatus(jobId, "completed");
                log.info("[Worker] ✓ Job #{} ({}) completed on attempt {}", jobId, type, attempt);
                return;
            } catch (Exception e) {
                log.warn("[Worker] ✗ Job #{} ({}) failed attempt {}/{}: {}",
                    jobId, type, attempt, MAX_ATTEMPTS, e.getMessage());

                // If maximum retry threshold exceeded, record failure reason and mark status failed
                if (attempt >= MAX_ATTEMPTS) {
                    queueService.setFailedReason(jobId, e.getMessage());
                    queueService.setStatus(jobId, "failed");
                    log.error("[Worker] ✗ Job #{} ({}) permanently failed after {} attempts",
                        jobId, type, MAX_ATTEMPTS);
                } else {
                    // Exponential backoff delay calculation: 500ms, 1000ms, 2000ms...
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

    // Dispatches task execution to appropriate service method by mapping the job type
    private Object dispatch(String jobId, String type) throws Exception {
        String payloadJson = queueService.getPayloadJson(jobId);
        return switch (type) {
            // Parses and executes a job application task
            case "apply-to-job" -> {
                ApplyToJobPayload p = objectMapper.readValue(payloadJson, ApplyToJobPayload.class);
                yield jobSeekerService.applyToJob(
                    UUID.fromString(p.getUserId()),
                    UUID.fromString(p.getJobId()),
                    p.getCoverLetter()
                );
            }
            // Parses and executes a job bookmark task
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
