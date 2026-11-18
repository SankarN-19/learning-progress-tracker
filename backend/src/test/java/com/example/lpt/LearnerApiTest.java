package com.example.lpt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.lpt.entity.Account;
import com.example.lpt.entity.Role;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/** FR-3 and FR-7: learners list; adding a candidate with one-time credentials; coordinator reset. */
class LearnerApiTest extends AbstractApiTest {

    private static final String URL = "/api/v1/learners";

    @Test
    void addCandidateReturnsOneTimeCredentials() throws Exception {
        MvcResult result = send(HttpMethod.POST, URL, coordinator(),
                Map.of("name", "Jane Doe", "email", "Jane.Doe@Example.TEST", "cohort", "Backend cohort"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.learner.displayName").value("Jane Doe"))
                .andExpect(jsonPath("$.learner.cohort").value("Backend cohort"))
                .andExpect(jsonPath("$.learner.email").value("jane.doe@example.test"))
                .andExpect(jsonPath("$.credentials.username").value("jane.doe@example.test"))
                .andReturn();

        JsonNode credentials = body(result).get("credentials");
        String temporaryPassword = credentials.get("temporaryPassword").asText();
        assertThat(temporaryPassword).hasSize(14);
        assertThat(Instant.parse(credentials.get("expiresAt").asText()))
                .isBetween(Instant.now().plus(Duration.ofHours(23)), Instant.now().plus(Duration.ofHours(25)));

        Account stored = accounts.findByEmail("jane.doe@example.test").orElseThrow();
        assertThat(stored.getRole()).isEqualTo(Role.CANDIDATE);
        assertThat(stored.isMustChangePassword()).isTrue();
        assertThat(stored.getPasswordHash()).startsWith("$2").isNotEqualTo(temporaryPassword); // hashed, never plain
        assertThat(result.getResponse().getContentAsString().toLowerCase()).doesNotContain("passwordhash");
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() throws Exception {
        MockHttpSession coordinator = coordinator();
        createCandidate(coordinator, "Jane Doe", "jane@example.test", "Cohort");

        send(HttpMethod.POST, URL, coordinator, Map.of("name", "Other Jane", "email", "JANE@Example.Test", "cohort", "Cohort"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A candidate with this email already exists."))
                .andExpect(jsonPath("$.errors.email").exists());

        long candidates = accounts.findAll().stream().filter(a -> a.getRole() == Role.CANDIDATE).count();
        assertThat(candidates).isEqualTo(1);
    }

    @Test
    void emailOfExistingCoordinatorIsAlsoDuplicate() throws Exception {
        send(HttpMethod.POST, URL, coordinator(), Map.of("name", "Sneaky", "email", COORDINATOR_EMAIL, "cohort", "C"))
                .andExpect(status().isConflict());
    }

    @Test
    void addCandidateRejectsMissingOrInvalidFields() throws Exception {
        MockHttpSession coordinator = coordinator();
        send(HttpMethod.POST, URL, coordinator, Map.of("email", "a@example.test", "cohort", "C"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists());
        send(HttpMethod.POST, URL, coordinator, Map.of("name", "A", "cohort", "C"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").exists());
        send(HttpMethod.POST, URL, coordinator, Map.of("name", "A", "email", "a@example.test"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.cohort").exists());
        send(HttpMethod.POST, URL, coordinator, Map.of("name", "   ", "email", "a@example.test", "cohort", "C"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists());
        send(HttpMethod.POST, URL, coordinator, Map.of("name", "A", "email", "not-an-email", "cohort", "C"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Enter a valid email address."));
    }

    @Test
    void learnersListShowsProgressCounts() throws Exception {
        MockHttpSession coordinator = coordinator();
        long first = createActivity(coordinator, "Activity one", "Topic", 2);
        long second = createActivity(coordinator, "Activity two", "Topic", 3);
        Candidate ann = activeCandidate(coordinator, "Ann Lee", "ann@example.test");
        createCandidate(coordinator, "Bo Park", "bo@example.test", "Cohort");

        long annFirst = assign(coordinator, ann.learnerId(), first);
        assign(coordinator, ann.learnerId(), second);
        setStatus(ann, annFirst, "Completed");

        MvcResult result = getAs(URL, coordinator).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andReturn();
        JsonNode annRow = row(body(result), "Ann Lee");
        JsonNode boRow = row(body(result), "Bo Park");
        assertThat(annRow.get("assignedCount").asInt()).isEqualTo(2);
        assertThat(annRow.get("completedCount").asInt()).isEqualTo(1);
        assertThat(boRow.get("assignedCount").asInt()).isZero();
        assertThat(boRow.get("completedCount").asInt()).isZero();
    }

    @Test
    void resetPasswordIssuesNewOneTimeCredentials() throws Exception {
        MockHttpSession coordinator = coordinator();
        Candidate candidate = activeCandidate(coordinator, "Reset Me", "reset@example.test");

        MvcResult result = send(HttpMethod.POST, URL + "/" + candidate.learnerId() + "/reset-password", coordinator, null)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.credentials.username").value("reset@example.test"))
                .andReturn();
        String newTemporary = body(result).at("/credentials/temporaryPassword").asText();
        assertThat(newTemporary).hasSize(14).isNotEqualTo(candidate.password());

        send(HttpMethod.POST, LOGIN, null, creds(candidate.email(), candidate.password()))
                .andExpect(status().isUnauthorized());                         // old password no longer works
        send(HttpMethod.POST, LOGIN, null, creds(candidate.email(), newTemporary))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));       // must choose a new one again
    }

    @Test
    void resetPasswordEndsExistingSessions() throws Exception {
        MockHttpSession coordinator = coordinator();
        Candidate candidate = activeCandidate(coordinator, "Signed In", "signedin@example.test");
        getAs("/api/v1/me/assignments", candidate.session()).andExpect(status().isOk());

        send(HttpMethod.POST, URL + "/" + candidate.learnerId() + "/reset-password", coordinator, null)
                .andExpect(status().isOk());

        getAs("/api/v1/me/assignments", candidate.session())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your session has ended. Please sign in again."));
    }

    @Test
    void resetPasswordRestoresAccessAfterExpiry() throws Exception {
        MockHttpSession coordinator = coordinator();
        Created created = createCandidate(coordinator, "Too Slow", "slow@example.test", "Cohort");
        expireTemporaryPassword(created.email());
        send(HttpMethod.POST, LOGIN, null, creds(created.email(), created.temporaryPassword()))
                .andExpect(status().isUnauthorized());                         // expired credentials rejected

        MvcResult reset = send(HttpMethod.POST, URL + "/" + created.learnerId() + "/reset-password", coordinator, null)
                .andExpect(status().isOk()).andReturn();
        String fresh = body(reset).at("/credentials/temporaryPassword").asText();

        send(HttpMethod.POST, LOGIN, null, creds(created.email(), fresh)).andExpect(status().isOk());
    }

    @Test
    void deleteRemovesLearnerAccountAssignmentsAndSessions() throws Exception {
        MockHttpSession coordinator = coordinator();
        long activity = createActivity(coordinator, "Stays behind", "Topic", 2);
        Candidate ann = activeCandidate(coordinator, "Ann Lee", "ann@example.test");
        assign(coordinator, ann.learnerId(), activity);

        send(HttpMethod.DELETE, URL + "/" + ann.learnerId(), coordinator, null).andExpect(status().isNoContent());

        getAs(URL, coordinator).andExpect(jsonPath("$.length()").value(0));
        getAs("/api/v1/cohort-summary", coordinator)
                .andExpect(jsonPath("$.learners").value(0)).andExpect(jsonPath("$.assignments").value(0));
        getAs("/api/v1/activities", coordinator).andExpect(jsonPath("$.length()").value(1)); // the activity stays
        getAs("/api/v1/me/assignments", ann.session()).andExpect(status().isUnauthorized()); // live session ended
        send(HttpMethod.POST, LOGIN, null, creds(ann.email(), ann.password())).andExpect(status().isUnauthorized());
        assertThat(accounts.findByEmail(ann.email())).isEmpty();

        createCandidate(coordinator, "Ann Again", ann.email(), "Cohort"); // the email can be reused
    }

    @Test
    void deleteUnknownLearnerIsNotFound() throws Exception {
        send(HttpMethod.DELETE, URL + "/999999", coordinator(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Learner not found."));
    }

    @Test
    void resetPasswordForUnknownLearnerIsNotFound() throws Exception {
        send(HttpMethod.POST, URL + "/999999/reset-password", coordinator(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Learner not found."));
    }

    private static JsonNode row(JsonNode array, String displayName) {
        return StreamSupport.stream(array.spliterator(), false)
                .filter(n -> n.get("displayName").asText().equals(displayName))
                .findFirst().orElseThrow();
    }
}
