package com.jobfinder.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Metadata stored in a Redis hash for each queued job.
 * Mirrors the Bull job object shape so the frontend polling code
 * works without modification.
 *
 * Key pattern: "queue-meta:{jobId}"
 * Fields: type, status, attemptsMade, createdAt, payload (JSON), result (JSON), failedReason
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueJobRecord {

    private String  id;
    private String  type;
    private String  status;       // waiting | active | completed | failed
    private int     attemptsMade;
    private Instant createdAt;
    private String  payloadJson;  // serialized QueueJobPayload
    private String  resultJson;   // serialized result on success
    private String  failedReason; // error message on failure
}
