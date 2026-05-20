package com.jobfinder.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Replaces ApplyToJobPayload interface from types.ts */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyToJobPayload implements QueueJobPayload {

    @Builder.Default
    private String type = "apply-to-job";
    private String userId;
    private String jobId;
    private String coverLetter; // optional
}
