package com.jobfinder.dto.response;

import java.util.List;

public record PagedJobResponse(
    List<JobResponse> jobs,
    long total,
    int page,
    int pageSize,
    int totalPages
) {}
