package com.example.lpt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.lpt.entity.Account;
import com.example.lpt.repository.AccountRepository;
import com.example.lpt.security.LoginRateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base for the MockMvc tests: the real Spring context, security filter chain and an in-memory
 * database. Every test starts from an empty database (the coordinator stays) and a fresh rate limiter.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractApiTest {

    protected static final String LOGIN = "/api/v1/auth/login";
    protected static final String COORDINATOR_EMAIL = "coordinator@example.test";
    protected static final String COORDINATOR_PASSWORD = "Test-Coordinator-Passw0rd";
    protected static final String NEW_PASSWORD = "My-New-Passw0rd!";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected AccountRepository accounts;
    @Autowired protected SessionRegistry sessionRegistry;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private LoginRateLimiter rateLimiter;

    /** A candidate created by the coordinator, with the one-time credentials. */
    protected record Created(long learnerId, String email, String temporaryPassword) {
    }

    /** A candidate who has finished first login (new password set) and holds a live session. */
    protected record Candidate(long learnerId, String email, String password, MockHttpSession session) {
    }

    @BeforeEach
    protected void cleanDatabaseAndLimiter() {
        jdbc.update("DELETE FROM assignments");
        jdbc.update("DELETE FROM learners");
        jdbc.update("DELETE FROM accounts WHERE role = 'CANDIDATE'");
        jdbc.update("DELETE FROM activities");
        ((Map<?, ?>) ReflectionTestUtils.getField(rateLimiter, "failures")).clear();
    }

    // ---------------------------------------------------------------- requests

    /** Sends a request with a valid CSRF token, an optional session and an optional JSON body. */
    protected ResultActions send(HttpMethod method, String url, MockHttpSession session, Object body)
            throws Exception {
        MockHttpServletRequestBuilder request = request(method, url).with(csrf());
        if (session != null) {
            request.session(session);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content(body instanceof String text ? text : json.writeValueAsString(body));
        }
        return mvc.perform(request);
    }

    protected ResultActions getAs(String url, MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder request = get(url);
        if (session != null) {
            request.session(session);
        }
        return mvc.perform(request);
    }

    protected static Map<String, String> creds(String email, String password) {
        return Map.of("email", email, "password", password);
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    // ---------------------------------------------------------------- scenario helpers

    protected MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = send(HttpMethod.POST, LOGIN, null, creds(email, password)).andReturn();
        assertThat(result.getResponse().getStatus()).as("login status for %s", email).isEqualTo(200);
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected MockHttpSession coordinator() throws Exception {
        return login(COORDINATOR_EMAIL, COORDINATOR_PASSWORD);
    }

    protected Created createCandidate(MockHttpSession coordinator, String name, String email, String cohort)
            throws Exception {
        MvcResult result = send(HttpMethod.POST, "/api/v1/learners", coordinator,
                Map.of("name", name, "email", email, "cohort", cohort))
                .andExpect(status().isCreated()).andReturn();
        JsonNode node = body(result);
        return new Created(node.at("/learner/id").asLong(),
                node.at("/credentials/username").asText(),
                node.at("/credentials/temporaryPassword").asText());
    }

    /** Creates a candidate, signs in with the temporary password and changes it. */
    protected Candidate activeCandidate(MockHttpSession coordinator, String name, String email) throws Exception {
        Created created = createCandidate(coordinator, name, email, "Test cohort");
        MockHttpSession firstLogin = login(created.email(), created.temporaryPassword());
        MvcResult changed = send(HttpMethod.POST, "/api/v1/auth/change-password", firstLogin,
                Map.of("currentPassword", created.temporaryPassword(), "newPassword", NEW_PASSWORD))
                .andExpect(status().isNoContent()).andReturn();
        MockHttpSession fresh = (MockHttpSession) changed.getRequest().getSession(false);
        return new Candidate(created.learnerId(), created.email(), NEW_PASSWORD, fresh);
    }

    protected long createActivity(MockHttpSession coordinator, String title, String topic, double hours)
            throws Exception {
        MvcResult result = send(HttpMethod.POST, "/api/v1/activities", coordinator,
                Map.of("title", title, "topic", topic, "plannedHours", hours))
                .andExpect(status().isCreated()).andReturn();
        return body(result).get("id").asLong();
    }

    protected long assign(MockHttpSession coordinator, long learnerId, long activityId) throws Exception {
        MvcResult result = send(HttpMethod.POST, "/api/v1/assignments", coordinator,
                Map.of("learnerId", learnerId, "activityId", activityId))
                .andExpect(status().isCreated()).andReturn();
        return body(result).get("id").asLong();
    }

    protected void setStatus(Candidate candidate, long assignmentId, String status) throws Exception {
        send(HttpMethod.PATCH, "/api/v1/assignments/" + assignmentId + "/status", candidate.session(),
                Map.of("status", status)).andExpect(status().isOk());
    }

    /** Pushes the temporary-password expiry into the past (through the repository, so time zones agree). */
    protected void expireTemporaryPassword(String email) {
        Account account = accounts.findByEmail(email).orElseThrow();
        account.setTempPasswordExpiresAt(Instant.now().minus(Duration.ofDays(3)));
        accounts.save(account);
    }
}
