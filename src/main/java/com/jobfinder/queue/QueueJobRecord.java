package com.jobfinder.queue;

// Lombok annotations for generating getters, setters, builders, and constructors
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Standard Java class for representing timestamps
import java.time.Instant;

/**
 * Metadata stored in a Redis hash for each queued job.
 * Mirrors the Bull job object shape so the frontend polling code
 * works without modification.
 *
 * Key pattern: "queue-meta:{jobId}"
 * Fields: type, status, attemptsMade, createdAt, payload (JSON), result (JSON), failedReason
 */
// Generates standard getter, setter, toString, equals, and hashCode methods
@Data
// Generates builder pattern API for the metadata class
@Builder
// Generates no-argument constructor
@NoArgsConstructor
// Generates all-arguments constructor
@AllArgsConstructor
public class QueueJobRecord {

    // Unique job identifier string
    private String  id;
    // Type name of the background job (e.g. apply-to-job)
    private String  type;
    // Current status state: waiting | active | completed | failed
    private String  status;
    // Count of execution attempts made
    private int     attemptsMade;
    // Timestamp when the job was added to the queue
    private Instant createdAt;
    // JSON-serialized string representing the QueueJobPayload
    private String  payloadJson;
    // JSON-serialized string representing successful result output
    private String  resultJson;
    // Explanation error message if status is failed
    private String  failedReason;
}
