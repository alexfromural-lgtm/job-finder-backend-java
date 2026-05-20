package com.jobfinder.queue;

/**
 * Marker interface for all queued DB write operation payloads.
 * Replaces the QueuePayload discriminated union from types.ts.
 */
public interface QueueJobPayload {
    String getType();
    String getUserId();
}
