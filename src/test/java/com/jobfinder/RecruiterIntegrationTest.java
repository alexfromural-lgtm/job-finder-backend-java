package com.jobfinder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecruiterIntegrationTest extends BaseIntegrationTest {

    private String recruiterCookies;
    private String seekerCookies;
    private String jobId;

    @BeforeEach
    void setup() throws Exception {
        String tag = String.valueOf(System.nanoTime());
        recruiterCookies = cookieHeader(
            signupRecruiterAndGetCookies("Rec " + tag, "rec" + tag + "@test.com", "pass1234", "BigCorp"));
        seekerCookies = cookieHeader(
            signupJobSeekerAndGetCookies("Sk " + tag, "sk" + tag + "@test.com", "pass1234"));
        String jobResp = mockMvc.perform(post("/api/jobs")
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "Job " + tag, "description", "desc",
                    "requirements", "req", "location", "NY"))))
            .andReturn().getResponse().getContentAsString();
        jobId = objectMapper.readTree(jobResp).get("id").asText();
    }

    @Test
    void get_recruiter_profile_returns_company_info() throws Exception {
        mockMvc.perform(get("/api/recruiter/profile").header("Cookie", recruiterCookies))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyName").exists());
    }

    @Test
    void update_recruiter_profile_persists_changes() throws Exception {
        mockMvc.perform(patch("/api/recruiter/profile")
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "description", "We are awesome", "industry", "Technology"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("We are awesome"))
            .andExpect(jsonPath("$.industry").value("Technology"));
    }

    @Test
    void get_applications_for_job_returns_list_after_apply() throws Exception {
        mockMvc.perform(post("/api/jobseeker/apply/" + jobId)
                .header("Cookie", seekerCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("coverLetter", "Hire me"))))
            .andExpect(status().isAccepted());
        Thread.sleep(1500);
        mockMvc.perform(get("/api/recruiter/jobs/" + jobId + "/applications")
                .header("Cookie", recruiterCookies))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].jobId").value(jobId));
    }

    @Test
    void update_application_status_to_shortlisted() throws Exception {
        mockMvc.perform(post("/api/jobseeker/apply/" + jobId)
                .header("Cookie", seekerCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("coverLetter", "Please"))))
            .andExpect(status().isAccepted());
        Thread.sleep(1500);
        String appsResp = mockMvc.perform(get("/api/recruiter/jobs/" + jobId + "/applications")
                .header("Cookie", recruiterCookies))
            .andReturn().getResponse().getContentAsString();
        String applicationId = objectMapper.readTree(appsResp).get(0).get("id").asText();
        mockMvc.perform(patch("/api/recruiter/applications/" + applicationId + "/status")
                .header("Cookie", recruiterCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "shortlisted"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("shortlisted"));
    }
}
