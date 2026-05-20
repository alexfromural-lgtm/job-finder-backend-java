package com.jobfinder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Shared base for all integration tests.
 *
 * - Starts one PostgreSQL 16 and one Redis 7 container for the entire test suite
 *   (static @Container = reused across test classes → fast).
 * - @DynamicPropertySource injects their ports into Spring before context loads.
 * - Flyway runs V1 + V2 automatically on the test DB.
 * - @Transactional on individual tests can roll back DB changes, but is NOT
 *   used here by default so the queue worker (running in a separate thread) can
 *   read writes made during the test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("job_finder_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.url",
            () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // -------------------------------------------------------------------
    // Helper: sign up a job seeker and return Set-Cookie header values
    // -------------------------------------------------------------------
    protected String[] signupJobSeekerAndGetCookies(String name, String email, String password)
            throws Exception {
        MvcResult result = mockMvc.perform(
            post("/api/auth/signup/jobseeker")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("name", name, "email", email, "password", password))))
            .andReturn();
        return extractCookieHeaders(result.getResponse());
    }

    // -------------------------------------------------------------------
    // Helper: sign up a recruiter and return Set-Cookie header values
    // -------------------------------------------------------------------
    protected String[] signupRecruiterAndGetCookies(String name, String email, String password,
                                                     String companyName) throws Exception {
        MvcResult result = mockMvc.perform(
            post("/api/auth/signup/recruiter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", name, "email", email, "password", password,
                    "companyName", companyName))))
            .andReturn();
        return extractCookieHeaders(result.getResponse());
    }

    // -------------------------------------------------------------------
    // Helper: login and return Set-Cookie header values
    // -------------------------------------------------------------------
    protected String[] loginAndGetCookies(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", email, "password", password))))
            .andReturn();
        return extractCookieHeaders(result.getResponse());
    }

    // -------------------------------------------------------------------
    // Helper: build a "Cookie: ..." header string from Set-Cookie values
    // -------------------------------------------------------------------
    protected String cookieHeader(String[] setCookieHeaders) {
        return Arrays.stream(setCookieHeaders)
            .map(h -> h.split(";")[0]) // take only "name=value" part
            .reduce((a, b) -> a + "; " + b)
            .orElse("");
    }

    private String[] extractCookieHeaders(MockHttpServletResponse response) {
        return response.getHeaders("Set-Cookie").toArray(new String[0]);
    }
}
