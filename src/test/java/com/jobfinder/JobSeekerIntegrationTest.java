package com.jobfinder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for /api/jobseeker endpoints.
 */
class JobSeekerIntegrationTest extends BaseIntegrationTest {

    private String seekerCookies;
    private String jobId;

    @BeforeEach
    void setup() throws Exception {
        String tag = String.valueOf(System.nanoTime());

        seekerCookies = cookieHeader(
            signupJobSeekerAndGetCookies("Seeker " + tag, "sk" + tag + "@test.com", "pass1234"));

        // Create a recruiter and a job for apply/save tests
        String recruiterCookies = cookieHeader(
            signupRecruiterAndGetCookies("Rec " + tag, "rec" + tag + "@test.com", "pass1234", "Corp"));

        String jobResp = mockMvc.perform(post("/api/jobs")
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "Test Job " + tag, "description", "desc",
                    "requirements", "req", "location", "Remote"))))
            .andReturn().getResponse().getContentAsString();

        jobId = objectMapper.readTree(jobResp).get("id").asText();
    }

    // ------------------------------------------------------------------
    // GET /api/jobseeker/profile
    // ------------------------------------------------------------------

    @Test
    void get_profile_returns_seeker_profile() throws Exception {
        mockMvc.perform(get("/api/jobseeker/profile")
                .header("Cookie", seekerCookies))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists());
    }

    // ------------------------------------------------------------------
    // PATCH /api/jobseeker/profile
    // ------------------------------------------------------------------

    @Test
    void update_profile_persists_bio_and_skills() throws Exception {
        mockMvc.perform(patch("/api/jobseeker/profile")
                .header("Cookie", seekerCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "bio", "Java developer",
                    "skills", new String[]{"Java", "Spring"}))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bio").value("Java developer"))
            .andExpect(jsonPath("$.skills", hasItems("Java", "Spring")));
    }

    // ------------------------------------------------------------------
    // POST /api/jobseeker/apply/{jobId} → 202 + jobId
    // ------------------------------------------------------------------

    @Test
    void apply_to_job_returns202_with_queue_job_id() throws Exception {
        mockMvc.perform(post("/api/jobseeker/apply/" + jobId)
                .header("Cookie", seekerCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("coverLetter", "Please hire me"))))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobId").exists())
            .andExpect(jsonPath("$.message").exists());
    }

    // ------------------------------------------------------------------
    // GET /api/jobseeker/applications
    // ------------------------------------------------------------------

    @Test
    void get_applications_returns_list() throws Exception {
        // Apply first
        mockMvc.perform(post("/api/jobseeker/apply/" + jobId)
                .header("Cookie", seekerCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isAccepted());

        // Wait briefly for worker to process
        Thread.sleep(1500);

        mockMvc.perform(get("/api/jobseeker/applications")
                .header("Cookie", seekerCookies))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ------------------------------------------------------------------
    // POST /api/jobseeker/saved/{jobId} → 202
    // ------------------------------------------------------------------

    @Test
    void save_job_returns202_with_queue_job_id() throws Exception {
        mockMvc.perform(post("/api/jobseeker/saved/" + jobId)
                .header("Cookie", seekerCookies))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobId").exists());
    }

    // ------------------------------------------------------------------
    // DELETE /api/jobseeker/saved/{jobId}
    // ------------------------------------------------------------------

    @Test
    void unsave_job_returns204() throws Exception {
        // Save first, wait for worker
        mockMvc.perform(post("/api/jobseeker/saved/" + jobId)
                .header("Cookie", seekerCookies))
            .andExpect(status().isAccepted());

        Thread.sleep(1500);

        mockMvc.perform(delete("/api/jobseeker/saved/" + jobId)
                .header("Cookie", seekerCookies))
            .andExpect(status().isNoContent());
    }
}
