package com.jobfinder;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for all /api/auth endpoints.
 * Each test creates its own unique user data — no cross-test dependencies.
 */
class AuthIntegrationTest extends BaseIntegrationTest {

    // ------------------------------------------------------------------
    // Signup — Job Seeker
    // ------------------------------------------------------------------

    @Test
    void signup_jobSeeker_returns201_and_sets_cookies() throws Exception {
        mockMvc.perform(post("/api/auth/signup/jobseeker")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Test Seeker",
                    "email", unique("seeker") + "@test.com",
                    "password", "pass1234"))))
            .andExpect(status().isCreated())
            .andExpect(cookie().exists("accessToken"))
            .andExpect(cookie().exists("refreshToken"))
            .andExpect(cookie().httpOnly("accessToken", true))
            .andExpect(cookie().httpOnly("refreshToken", true))
            .andExpect(jsonPath("$.roles[0]").value("JOB_SEEKER"));
    }

    // ------------------------------------------------------------------
    // Signup — Recruiter
    // ------------------------------------------------------------------

    @Test
    void signup_recruiter_returns201_and_sets_cookies() throws Exception {
        mockMvc.perform(post("/api/auth/signup/recruiter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Test Recruiter",
                    "email", unique("recruiter") + "@test.com",
                    "password", "pass1234",
                    "companyName", "Test Corp"))))
            .andExpect(status().isCreated())
            .andExpect(cookie().exists("accessToken"))
            .andExpect(jsonPath("$.roles[0]").value("RECRUITER"));
    }

    // ------------------------------------------------------------------
    // Signup — Duplicate email → 409
    // ------------------------------------------------------------------

    @Test
    void signup_duplicate_email_returns409() throws Exception {
        String email = unique("dup") + "@test.com";
        signupJobSeekerAndGetCookies("Dup User", email, "pass1234");

        mockMvc.perform(post("/api/auth/signup/jobseeker")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Dup User 2", "email", email, "password", "pass1234"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Email already in use"));
    }

    // ------------------------------------------------------------------
    // Signup — Validation failure (no email) → 400
    // ------------------------------------------------------------------

    @Test
    void signup_missing_email_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup/jobseeker")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "No Email", "password", "pass1234"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.fields.email").exists());
    }

    // ------------------------------------------------------------------
    // Login — valid credentials
    // ------------------------------------------------------------------

    @Test
    void login_valid_credentials_returns200_and_sets_cookies() throws Exception {
        String email = unique("login") + "@test.com";
        signupJobSeekerAndGetCookies("Login User", email, "pass1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", email, "password", "pass1234"))))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("accessToken"))
            .andExpect(cookie().exists("refreshToken"))
            .andExpect(jsonPath("$.userId").exists());
    }

    // ------------------------------------------------------------------
    // Login — wrong password → 401
    // ------------------------------------------------------------------

    @Test
    void login_wrong_password_returns401() throws Exception {
        String email = unique("badpw") + "@test.com";
        signupJobSeekerAndGetCookies("Bad PW", email, "pass1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", email, "password", "wrongpassword"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    // ------------------------------------------------------------------
    // GET /api/auth/me — authenticated
    // ------------------------------------------------------------------

    @Test
    void get_me_with_valid_cookie_returns_current_user() throws Exception {
        String email = unique("me") + "@test.com";
        String[] cookies = signupJobSeekerAndGetCookies("Me User", email, "pass1234");

        mockMvc.perform(get("/api/auth/me")
                .header("Cookie", cookieHeader(cookies)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.roles[0]").value("JOB_SEEKER"));
    }

    // ------------------------------------------------------------------
    // GET /api/auth/me — no cookie → 401
    // ------------------------------------------------------------------

    @Test
    void get_me_without_cookie_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // POST /api/auth/logout — clears cookies
    // ------------------------------------------------------------------

    @Test
    void logout_clears_cookies() throws Exception {
        String[] cookies = signupJobSeekerAndGetCookies(
            "Logout", unique("logout") + "@test.com", "pass1234");

        mockMvc.perform(post("/api/auth/logout")
                .header("Cookie", cookieHeader(cookies)))
            .andExpect(status().isOk())
            .andExpect(cookie().maxAge("accessToken", 0))
            .andExpect(cookie().maxAge("refreshToken", 0));
    }

    // ------------------------------------------------------------------
    // POST /api/auth/refresh — issues new tokens
    // ------------------------------------------------------------------

    @Test
    void refresh_with_valid_refresh_cookie_issues_new_tokens() throws Exception {
        String[] cookies = signupJobSeekerAndGetCookies(
            "Refresh", unique("refresh") + "@test.com", "pass1234");

        // Extract only the refreshToken cookie for the refresh call
        String refreshCookie = java.util.Arrays.stream(cookies)
            .filter(c -> c.startsWith("refreshToken"))
            .findFirst().orElseThrow();

        mockMvc.perform(post("/api/auth/refresh")
                .header("Cookie", refreshCookie.split(";")[0]))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("accessToken"))
            .andExpect(cookie().exists("refreshToken"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private String unique(String prefix) {
        return prefix + "_" + System.nanoTime();
    }
}
