package com.jobfinder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for /api/jobs endpoints.
 */
class JobIntegrationTest extends BaseIntegrationTest {

    private String recruiterCookies;
    private String seekerCookies;

    @BeforeEach
    void setup() throws Exception {
        String tag = String.valueOf(System.nanoTime());
        recruiterCookies = cookieHeader(
            signupRecruiterAndGetCookies("R " + tag, "r" + tag + "@test.com", "pass1234", "Corp " + tag));
        seekerCookies = cookieHeader(
            signupJobSeekerAndGetCookies("S " + tag, "s" + tag + "@test.com", "pass1234"));
    }

    // ------------------------------------------------------------------
    // GET /api/jobs/all — public, paginated
    // ------------------------------------------------------------------

    @Test
    void get_all_jobs_returns200_without_auth() throws Exception {
        mockMvc.perform(get("/api/jobs/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobs").isArray())
            .andExpect(jsonPath("$.total").isNumber())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.totalPages").isNumber());
    }

    // ------------------------------------------------------------------
    // POST /api/jobs — recruiter creates a job
    // ------------------------------------------------------------------

    @Test
    void create_job_as_recruiter_returns201() throws Exception {
        mockMvc.perform(post("/api/jobs")
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "Java Engineer",
                    "description", "Build microservices",
                    "requirements", "5+ years Java",
                    "location", "Remote"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("Java Engineer"));
    }

    // ------------------------------------------------------------------
    // POST /api/jobs — no auth → 401
    // ------------------------------------------------------------------

    @Test
    void create_job_without_auth_returns401() throws Exception {
        mockMvc.perform(post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "Ghost Job", "description", "x",
                    "requirements", "x", "location", "x"))))
            .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // POST /api/jobs — seeker tries to create → 403
    // ------------------------------------------------------------------

    @Test
    void create_job_as_seeker_returns403() throws Exception {
        mockMvc.perform(post("/api/jobs")
                .header("Cookie", seekerCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "X", "description", "x",
                    "requirements", "x", "location", "x"))))
            .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // GET /api/jobs/{id} — public
    // ------------------------------------------------------------------

    @Test
    void get_job_by_id_returns_correct_job() throws Exception {
        // Create a job first
        String response = mockMvc.perform(post("/api/jobs")
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "Backend Dev", "description", "desc",
                    "requirements", "req", "location", "NYC"))))
            .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/jobs/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.title").value("Backend Dev"));
    }

    // ------------------------------------------------------------------
    // GET /api/jobs/all?search= — filtered
    // ------------------------------------------------------------------

    @Test
    void search_jobs_returns_filtered_results() throws Exception {
        // Create a job with a unique title
        String uniqueTitle = "UniqueSearchTitle_" + System.nanoTime();
        mockMvc.perform(post("/api/jobs")
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", uniqueTitle, "description", "desc",
                    "requirements", "req", "location", "Remote"))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/jobs/all").param("search", uniqueTitle))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobs[0].title").value(uniqueTitle));
    }

    // ------------------------------------------------------------------
    // PUT /api/jobs/{id} — owner updates
    // ------------------------------------------------------------------

    @Test
    void update_job_as_owner_returns_updated_job() throws Exception {
        String response = mockMvc.perform(post("/api/jobs")
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "Old Title", "description", "desc",
                    "requirements", "req", "location", "SF"))))
            .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(put("/api/jobs/" + id)
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "New Title"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("New Title"));
    }

    // ------------------------------------------------------------------
    // DELETE /api/jobs/{id} — owner deletes
    // ------------------------------------------------------------------

    @Test
    void delete_job_as_owner_returns204() throws Exception {
        String response = mockMvc.perform(post("/api/jobs")
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "Delete Me", "description", "desc",
                    "requirements", "req", "location", "LA"))))
            .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(delete("/api/jobs/" + id)
                .header("Cookie", recruiterCookies))
            .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/api/jobs/" + id))
            .andExpect(status().isNotFound());
    }
}
