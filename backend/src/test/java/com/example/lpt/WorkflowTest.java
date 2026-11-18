package com.example.lpt;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The main workflow end to end, through the real security chain and database:
 * create candidate -> first login -> change password -> assign -> update status -> summary.
 */
class WorkflowTest extends AbstractApiTest {

    @Test
    void fullWorkflowFromCandidateCreationToCohortSummary() throws Exception {
        // 1. the coordinator signs in, creates an activity and adds a candidate
        MockHttpSession coordinator = coordinator();
        long activityId = createActivity(coordinator, "React Basics", "Frontend", 6);
        Created created = createCandidate(coordinator, "Jordan Lee", "Jordan.Lee@Example.test", "Frontend cohort");

        // 2. first login with the one-time credentials: password change is required
        MvcResult firstLogin = send(HttpMethod.POST, LOGIN, null, creds(created.email(), created.temporaryPassword()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn();
        MockHttpSession temporarySession = (MockHttpSession) firstLogin.getRequest().getSession(false);
        getAs("/api/v1/me/assignments", temporarySession).andExpect(status().isForbidden());

        // 3. the candidate changes the password and now sees their (still empty) training
        MvcResult changed = send(HttpMethod.POST, "/api/v1/auth/change-password", temporarySession,
                Map.of("currentPassword", created.temporaryPassword(), "newPassword", NEW_PASSWORD))
                .andExpect(status().isNoContent()).andReturn();
        MockHttpSession candidate = (MockHttpSession) changed.getRequest().getSession(false);
        getAs("/api/v1/me/assignments", candidate).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        // 4. the coordinator assigns the activity (a second try is a duplicate)
        long assignmentId = assign(coordinator, created.learnerId(), activityId);
        send(HttpMethod.POST, "/api/v1/assignments", coordinator,
                Map.of("learnerId", created.learnerId(), "activityId", activityId))
                .andExpect(status().isConflict());

        // 5. the candidate sees it and updates the progress
        getAs("/api/v1/me/assignments", candidate).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("Not Started"))
                .andExpect(jsonPath("$[0].activity.title").value("React Basics"));
        send(HttpMethod.PATCH, "/api/v1/assignments/" + assignmentId + "/status", candidate, Map.of("status", "In Progress"))
                .andExpect(status().isOk());
        getAs("/api/v1/cohort-summary", coordinator)
                .andExpect(jsonPath("$.completed").value(0)).andExpect(jsonPath("$.inProgress").value(1));
        send(HttpMethod.PATCH, "/api/v1/assignments/" + assignmentId + "/status", candidate, Map.of("status", "Completed"))
                .andExpect(status().isOk());

        // 6. the cohort summary and the learner view reflect the saved data
        getAs("/api/v1/cohort-summary", coordinator).andExpect(status().isOk())
                .andExpect(jsonPath("$.learners").value(1))
                .andExpect(jsonPath("$.assignments").value(1))
                .andExpect(jsonPath("$.completed").value(1));
        getAs("/api/v1/learners/" + created.learnerId() + "/assignments", coordinator)
                .andExpect(jsonPath("$[0].status").value("Completed"));

        // 7. logout ends the candidate's session
        send(HttpMethod.POST, "/api/v1/auth/logout", candidate, null).andExpect(status().isNoContent());
        getAs("/api/v1/me/assignments", candidate).andExpect(status().isUnauthorized());
    }
}
