package com.jobfinder.dto.response;

import java.util.List;

/**
 * Matches the frontend contract: { jobs: [...], meta: { total, page, pageSize, totalPages } }
 * The frontend's jobs.api.ts destructures `{ jobs, meta }` from the response body.
 */
public record PagedJobResponse(
    List<JobResponse> jobs,
    Meta meta
) {
    public record Meta(
        long total,
        int page,
        int pageSize,
        int totalPages
    ) {}
}
