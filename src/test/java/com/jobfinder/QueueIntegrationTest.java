package com.jobfinder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Redis-backed queue:
 *  - POST apply/save → 202 + jobId
 *  - GET /api/queue/job/{jobId} with retry until status = "completed"
 *
 * Tests are self-contained: each creates its own user + job data.
 */
class QueueIntegrationTest extends BaseIntegrationTest {

    private String seekerCookies;
    private String jobId;

    @BeforeEach
    void setup() throws Exception {
        String tag = String.valueOf(System.nanoTime());

        String recruiterCookies = cookieHeader(
            signupRecruiterAndGetCookies("QRec " + tag, "qrec" + tag + "@test.com", "pass1234", "QCorp"));

        seekerCookies = cookieHeader(
            signupJobSeekerAndGetCookies("QSeeker " + tag, "qsk" + tag + "@test.com", "pass1234"));

        String jobResp = mockMvc.perform(post("/api/jobs")
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "Queue Test Job " + tag,
                    "description", "desc",
                    "requirements", "req",
                    "location", "Remote"))))
            .andReturn().getResponse().getContentAsString();

        jobId = objectMapper.readTree(jobResp).get("id").asText();
    }

    // ------------------------------------------------------------------
    // Apply to job — enqueue → poll until completed
    // ------------------------------------------------------------------

    @Test
    void apply_to_job_enqueues_and_completes() throws Exception {
        // POST apply → 202 with a queue jobId
        String applyResp = mockMvc.perform(post("/api/jobseeker/apply/" + jobId)
                .header("Cookie", seekerCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("coverLetter", "Please hire me"))))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobId").exists())
            .andExpect(jsonPath("$.message").exists())
            .andReturn().getResponse().getContentAsString();

        String queueJobId = objectMapper.readTree(applyResp).get("jobId").asText();

        // Poll until status = "completed" (max ~5 s)
        pollUntilCompleted(queueJobId);
    }

    // ------------------------------------------------------------------
    // Save job — enqueue → poll until completed
    // ------------------------------------------------------------------

    @Test
    void save_job_enqueues_and_completes() throws Exception {
        // POST save → 202 with a queue jobId
        String saveResp = mockMvc.perform(post("/api/jobseeker/saved/" + jobId)
                .header("Cookie", seekerCookies))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobId").exists())
            .andReturn().getResponse().getContentAsString();

        String queueJobId = objectMapper.readTree(saveResp).get("jobId").asText();

        // Poll until status = "completed" (max ~5 s)
        pollUntilCompleted(queueJobId);
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    /**
     * Polls GET /api/queue/job/{queueJobId} up to 10 times (500 ms apart)
     * and asserts the final status is "completed".
     */
    private void pollUntilCompleted(String queueJobId) throws Exception {
        String status = "waiting";
        for (int i = 0; i < 10; i++) {
            Thread.sleep(500);
            String resp = mockMvc.perform(get("/api/queue/job/" + queueJobId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            status = objectMapper.readTree(resp).get("status").asText();
            if ("completed".equals(status) || "failed".equals(status)) {
                break;
            }
        }
        // Final assertion after polling
        mockMvc.perform(get("/api/queue/job/" + queueJobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("completed"));
    }
}
