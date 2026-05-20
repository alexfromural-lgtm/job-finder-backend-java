package com.jobfinder.controller;

import com.jobfinder.dto.response.QueueJobStatusResponse;
import com.jobfinder.queue.DbWriteQueueService;
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
@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final DbWriteQueueService queueService;

    // GET /api/queue/job/{jobId}
    @GetMapping("/job/{jobId}")
    public ResponseEntity<QueueJobStatusResponse> getJobStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(queueService.getJobStatus(jobId));
    }
}
