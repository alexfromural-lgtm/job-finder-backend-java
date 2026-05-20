package com.jobfinder.dto.request;

import java.util.List;

public record UpdateJobSeekerProfileRequest(
    String bio,
    String location,
    List<String> skills,
    String education,
    String experience,
    String resumeUrl
) {}
