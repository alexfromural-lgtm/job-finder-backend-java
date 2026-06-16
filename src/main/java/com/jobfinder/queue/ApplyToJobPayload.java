package com.jobfinder.queue;

// Lombok annotations for generating getters, setters, builder patterns, and constructors
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Replaces ApplyToJobPayload interface from types.ts */
// Generates getters, setters, toString, equals, and hashCode methods
@Data
// Generates a builder pattern API for the class
@Builder
// Generates a no-argument constructor
@NoArgsConstructor
// Generates an all-arguments constructor
@AllArgsConstructor
public class ApplyToJobPayload implements QueueJobPayload {

    // Default queue job type identifier
    @Builder.Default
    private String type = "apply-to-job";
    // Unique user identifier of the applicant
    private String userId;
    // Unique identifier of the job listing
    private String jobId;
    // Optional cover letter text submitted by the applicant
    private String coverLetter; // optional
}
