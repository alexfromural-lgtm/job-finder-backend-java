package com.jobfinder.dto.request;

// standard Java utility lists
import java.util.List;

// Represents the payload for updating a job seeker candidate's profile
public record UpdateJobSeekerProfileRequest(
    // The updated biography text
    String bio,
    // The updated location details
    String location,
    // The updated list of candidate skills
    List<String> skills,
    // The updated educational history details
    String education,
    // The updated professional experience details
    String experience,
    // The updated resume file URL
    String resumeUrl
) {}
