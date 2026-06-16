package com.jobfinder.queue;

/**
 * Marker interface for all queued DB write operation payloads.
 * Replaces the QueuePayload discriminated union from types.ts.
 */
// Represents the contract for all background queue message payloads
public interface QueueJobPayload {
    // Returns the job type string (e.g. "apply-to-job" or "save-job")
    String getType();
    // Returns the user identifier requesting the background task
    String getUserId();
}
