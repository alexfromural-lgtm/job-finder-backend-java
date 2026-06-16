package com.jobfinder.controller;

// Import our response DTO mapping background jobs status
import com.jobfinder.dto.response.QueueJobStatusResponse;
// Import the background write queue service bean
import com.jobfinder.queue.DbWriteQueueService;
// Lombok constructor generation annotation
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes the queue job status polling endpoint.
 * Replaces the Bull job status polling route in the Node.js backend.
 *
 * GET /api/queue/job/{jobId} is public (clients poll without auth)
 * matching the Node.js implementation.
 */
// Marks this class as a REST Controller handling HTTP requests
@RestController
// Maps the base request path URI for all endpoints in this controller
@RequestMapping("/api/queue")
// Generates standard constructor injecting dependencies automatically
@RequiredArgsConstructor
public class QueueController {

    // Inject DbWriteQueueService bean
    private final DbWriteQueueService queueService;

    // GET /api/queue/job/{jobId}
    // Returns the execution status details for an asynchronous background write task
    @GetMapping("/job/{jobId}")
    public ResponseEntity<QueueJobStatusResponse> getJobStatus(@PathVariable String jobId) {
        // Retrieve and return job status details from Redis
        return ResponseEntity.ok(queueService.getJobStatus(jobId));
    }
}
