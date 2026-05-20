package com.jobfinder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Spring Boot Actuator endpoints.
 *
 * Verifies:
 *  - /actuator/health  → 200 with status UP  (public)
 *  - /actuator/health/liveness  → 200 (public, Kubernetes liveness probe)
 *  - /actuator/health/readiness → 200 (public, Kubernetes readiness probe)
 *  - /actuator/metrics → 401 without authentication (requires auth)
 */
class ActuatorIntegrationTest extends BaseIntegrationTest {

    // ------------------------------------------------------------------
    // GET /actuator/health — public, should return UP
    // ------------------------------------------------------------------

    @Test
    void health_endpoint_returns_200_and_status_up() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    // ------------------------------------------------------------------
    // GET /actuator/health/liveness — Kubernetes liveness probe
    // ------------------------------------------------------------------

    @Test
    void liveness_probe_returns_200() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    // ------------------------------------------------------------------
    // GET /actuator/health/readiness — Kubernetes readiness probe
    // ------------------------------------------------------------------

    @Test
    void readiness_probe_returns_200() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    // ------------------------------------------------------------------
    // GET /actuator/metrics — requires authentication (returns 401 without cookie)
    // ------------------------------------------------------------------

    @Test
    void metrics_endpoint_requires_authentication() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isUnauthorized());
    }
}
