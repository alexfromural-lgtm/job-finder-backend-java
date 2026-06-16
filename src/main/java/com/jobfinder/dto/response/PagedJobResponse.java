package com.jobfinder.dto.response;

// standard Java utility lists
import java.util.List;

/**
 * Matches the frontend contract: { jobs: [...], meta: { total, page, pageSize, totalPages } }
 * The frontend's jobs.api.ts destructures `{ jobs, meta }` from the response body.
 */
// Represents a paginated list response of job listings matching frontend requirements
public record PagedJobResponse(
    // The list of jobs returned for the current page
    List<JobResponse> jobs,
    // The pagination metadata
    Meta meta
) {
    // Nested record containing pagination metrics
    public record Meta(
        // Total number of jobs matching the search query across all pages
        long total,
        // The current 1-indexed page number
        int page,
        // Number of items per page
        int pageSize,
        // The total number of pages available
        int totalPages
    ) {}
}
