package com.jobfinder.queue;

// Lombok annotations for generating getters, setters, builders, and constructors
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Replaces SaveJobPayload interface from types.ts */
// Generates standard getter, setter, toString, equals, and hashCode methods
@Data
// Generates builder pattern API for the payload class
@Builder
// Generates no-argument constructor
@NoArgsConstructor
// Generates all-arguments constructor
@AllArgsConstructor
public class SaveJobPayload implements QueueJobPayload {

    // Default queue job type identifier
    @Builder.Default
    private String type = "save-job";
    // Unique user ID bookmarking the job
    private String userId;
    // Unique identifier of the bookmarked job
    private String jobId;
}
