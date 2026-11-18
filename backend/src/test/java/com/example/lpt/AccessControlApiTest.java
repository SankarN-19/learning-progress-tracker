package com.example.lpt;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

/** Roles are enforced on the server: coordinator-only, candidate-only and logged-out callers. */
class AccessControlApiTest extends AbstractApiTest {

    private static final String ACTIVITY_JSON = "{\"title\":\"T\",\"topic\":\"X\",\"plannedHours\":2}";

    static Stream<Arguments> coordinatorOnlyCalls() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/v1/learners", null),
                Arguments.of(HttpMethod.POST, "/api/v1/learners",
                        "{\"name\":\"X\",\"email\":\"x@example.test\",\"cohort\":\"C\"}"),
                Arguments.of(HttpMethod.GET, "/api/v1/learners/1/assignments", null),
                Arguments.of(HttpMethod.POST, "/api/v1/learners/1/reset-password", null),
                Arguments.of(HttpMethod.POST, "/api/v1/assignments", "{\"learnerId\":1,\"activityId\":1}"),
                Arguments.of(HttpMethod.GET, "/api/v1/cohort-summary", null),
                Arguments.of(HttpMethod.GET, "/api/v1/activities", null),
                Arguments.of(HttpMethod.POST, "/api/v1/activities", ACTIVITY_JSON),
                Arguments.of(HttpMethod.PUT, "/api/v1/activities/1", ACTIVITY_JSON),
                Arguments.of(HttpMethod.DELETE, "/api/v1/activities/1", null),
                Arguments.of(HttpMethod.DELETE, "/api/v1/learners/1", null));
    }

    @ParameterizedTest(name = "a candidate gets 403 on {0} {1}")
    @MethodSource("coordinatorOnlyCalls")
    void candidatesCannotUseCoordinatorEndpoints(HttpMethod method, String url, String body) throws Exception {
        Candidate candidate = activeCandidate(coordinator(), "Cand One", "cand1@example.test");

        send(method, url, candidate.session(), body).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "a logged-out caller gets 401 on {0} {1}")
    @MethodSource("coordinatorOnlyCalls")
    void loggedOutCallersAreRejected(HttpMethod method, String url, String body) throws Exception {
        send(method, url, null, body).andExpect(status().isUnauthorized());
    }

    static Stream<Arguments> candidateOnlyCalls() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/v1/me/assignments", null),
                Arguments.of(HttpMethod.PATCH, "/api/v1/assignments/1/status", "{\"status\":\"Completed\"}"));
    }

    @ParameterizedTest(name = "a coordinator gets 403 on {0} {1}")
    @MethodSource("candidateOnlyCalls")
    void coordinatorsCannotUseCandidateEndpoints(HttpMethod method, String url, String body) throws Exception {
        send(method, url, coordinator(), body).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "a logged-out caller gets 401 on {0} {1}")
    @MethodSource("candidateOnlyCalls")
    void candidateEndpointsRejectLoggedOutCallers(HttpMethod method, String url, String body) throws Exception {
        send(method, url, null, body).andExpect(status().isUnauthorized());
    }
}
