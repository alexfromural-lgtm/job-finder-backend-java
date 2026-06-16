package com.jobfinder.dto.response;

// Standard Java class for handling date/time representations
import java.time.Instant;

/**
 * Returned by GET /api/queue/job/{jobId}.
 * Mirrors the Bull job status shape used by the Node.js backend.
 */
// Represents the status of a background job submitted to the Redis queue
public record QueueJobStatusResponse(
    // Unique identifier of the queued background job
    String id,
    // The type or category of the background job (e.g. email, PDF processing)
    String type,
    // The current status: waiting | active | completed | failed
    String status,
    // Total execution attempts made so far
    int attemptsMade,
    // Timestamp when the job was added to the queue
    Instant createdAt,
    // The result object returned by successful job execution (if completed)
    Object result,
    // The error message or failure explanation (if status is failed)
    String failedReason
) {}
