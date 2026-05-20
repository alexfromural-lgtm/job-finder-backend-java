package com.jobfinder.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Replaces SaveJobPayload interface from types.ts */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveJobPayload implements QueueJobPayload {

    @Builder.Default
    private String type = "save-job";
    private String userId;
    private String jobId;
}
