package com.jobfinder.dto.response;

import java.time.Instant;

/**
 * Returned by GET /api/queue/job/{jobId}.
 * Mirrors the Bull job status shape used by the Node.js backend.
 */
public record QueueJobStatusResponse(
    String id,
    String type,
    String status,   // waiting | active | completed | failed
    int attemptsMade,
    Instant createdAt,
    Object result,
    String failedReason
) {}
