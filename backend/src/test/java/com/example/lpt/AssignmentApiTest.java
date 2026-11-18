package com.example.lpt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/** FR-3, FR-4, FR-5 and the ownership rules of FR-8. */
class AssignmentApiTest extends AbstractApiTest {

    private static final String URL = "/api/v1/assignments";

    @Test
    void assignmentStartsAsNotStarted() throws Exception {
        MockHttpSession coordinator = coordinator();
        long activity = createActivity(coordinator, "React Basics", "Frontend", 6);
        Created learner = createCandidate(coordinator, "Ann Lee", "ann@example.test", "Cohort");

        send(HttpMethod.POST, URL, coordinator, Map.of("learnerId", learner.learnerId(), "activityId", activity))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Not Started"))
                .andExpect(jsonPath("$.learnerId").value(learner.learnerId()))
                .andExpect(jsonPath("$.activity.title").value("React Basics"));
    }

    @Test
    void duplicateAssignmentIsRejected() throws Exception {
        MockHttpSession coordinator = coordinator();
        long activity = createActivity(coordinator, "REST APIs", "Backend", 8);
        Created learner = createCandidate(coordinator, "Ann Lee", "ann@example.test", "Cohort");
        assign(coordinator, learner.learnerId(), activity);

        send(HttpMethod.POST, URL, coordinator, Map.of("learnerId", learner.learnerId(), "activityId", activity))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Assignment already exists."));

        getAs("/api/v1/learners/" + learner.learnerId() + "/assignments", coordinator)
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void sameActivityCanBeAssignedToAnotherLearner() throws Exception {
        MockHttpSession coordinator = coordinator();
        long activity = createActivity(coordinator, "REST APIs", "Backend", 8);
        Created ann = createCandidate(coordinator, "Ann Lee", "ann@example.test", "Cohort");
        Created bo = createCandidate(coordinator, "Bo Park", "bo@example.test", "Cohort");

        assign(coordinator, ann.learnerId(), activity);
        assign(coordinator, bo.learnerId(), activity); // 201 expected by the helper
    }

    @Test
    void assignRejectsUnknownLearnerOrActivity() throws Exception {
        MockHttpSession coordinator = coordinator();
        long activity = createActivity(coordinator, "REST APIs", "Backend", 8);
        Created learner = createCandidate(coordinator, "Ann Lee", "ann@example.test", "Cohort");

        send(HttpMethod.POST, URL, coordinator, Map.of("learnerId", 999999, "activityId", activity))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Learner not found."));
        send(HttpMethod.POST, URL, coordinator, Map.of("learnerId", learner.learnerId(), "activityId", 999999))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Activity not found."));
    }

    @Test
    void assignRejectsMissingIds() throws Exception {
        MockHttpSession coordinator = coordinator();
        send(HttpMethod.POST, URL, coordinator, Map.of("learnerId", 1))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.activityId").exists());
        send(HttpMethod.POST, URL, coordinator, Map.of("activityId", 1))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.learnerId").exists());
    }

    @Test
    void learnerAssignmentsIncludeActivityDetails() throws Exception {
        MockHttpSession coordinator = coordinator();
        long activity = createActivity(coordinator, "SQL Foundations", "Data", 4);
        Created learner = createCandidate(coordinator, "Ann Lee", "ann@example.test", "Cohort");
        assign(coordinator, learner.learnerId(), activity);

        getAs("/api/v1/learners/" + learner.learnerId() + "/assignments", coordinator)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("Not Started"))
                .andExpect(jsonPath("$[0].activity.title").value("SQL Foundations"))
                .andExpect(jsonPath("$[0].activity.topic").value("Data"))
                .andExpect(jsonPath("$[0].activity.plannedHours").value(4.0));

        getAs("/api/v1/learners/999999/assignments", coordinator)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Learner not found."));
    }

    @Test
    void candidateUpdatesOwnStatus() throws Exception {
        MockHttpSession coordinator = coordinator();
        long activity = createActivity(coordinator, "React Basics", "Frontend", 6);
        Candidate ann = activeCandidate(coordinator, "Ann Lee", "ann@example.test");
        long assignment = assign(coordinator, ann.learnerId(), activity);

        for (String status : new String[] {"In Progress", "Completed", "Not Started"}) {
            send(HttpMethod.PATCH, URL + "/" + assignment + "/status", ann.session(), Map.of("status", status))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(status));
        }
        setStatus(ann, assignment, "Completed");

        // saved: the coordinator and the candidate both read it back
        getAs("/api/v1/learners/" + ann.learnerId() + "/assignments", coordinator)
                .andExpect(jsonPath("$[0].status").value("Completed"));
        getAs("/api/v1/me/assignments", ann.session()).andExpect(jsonPath("$[0].status").value("Completed"));
    }

    @Test
    void invalidStatusIsRejected() throws Exception {
        MockHttpSession coordinator = coordinator();
        long activity = createActivity(coordinator, "React Basics", "Frontend", 6);
        Candidate ann = activeCandidate(coordinator, "Ann Lee", "ann@example.test");
        long assignment = assign(coordinator, ann.learnerId(), activity);

        send(HttpMethod.PATCH, URL + "/" + assignment + "/status", ann.session(), Map.of("status", "Finished"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Status must be Not Started, In Progress or Completed."));
        send(HttpMethod.PATCH, URL + "/" + assignment + "/status", ann.session(), Map.of("status", " "))
                .andExpect(status().isBadRequest());
        send(HttpMethod.PATCH, URL + "/" + assignment + "/status", ann.session(), "{}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAssignmentIsNotFound() throws Exception {
        Candidate ann = activeCandidate(coordinator(), "Ann Lee", "ann@example.test");

        send(HttpMethod.PATCH, URL + "/999999/status", ann.session(), Map.of("status", "Completed"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Assignment not found."));
    }

    @Test
    void candidateCannotUpdateAnotherCandidatesAssignment() throws Exception {
        MockHttpSession coordinator = coordinator();
        long activity = createActivity(coordinator, "React Basics", "Frontend", 6);
        Candidate ann = activeCandidate(coordinator, "Ann Lee", "ann@example.test");
        Candidate bo = activeCandidate(coordinator, "Bo Park", "bo@example.test");
        long boAssignment = assign(coordinator, bo.learnerId(), activity);

        // Ann changes the URL to Bo's assignment id
        send(HttpMethod.PATCH, URL + "/" + boAssignment + "/status", ann.session(), Map.of("status", "Completed"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only update your own training."));

        // nothing changed for Bo
        getAs("/api/v1/me/assignments", bo.session()).andExpect(jsonPath("$[0].status").value("Not Started"));
    }

    @Test
    void candidateSeesOnlyOwnAssignments() throws Exception {
        MockHttpSession coordinator = coordinator();
        long first = createActivity(coordinator, "Activity one", "Topic", 2);
        long second = createActivity(coordinator, "Activity two", "Topic", 3);
        Candidate ann = activeCandidate(coordinator, "Ann Lee", "ann@example.test");
        Candidate bo = activeCandidate(coordinator, "Bo Park", "bo@example.test");
        assign(coordinator, ann.learnerId(), first);
        assign(coordinator, bo.learnerId(), second);

        getAs("/api/v1/me/assignments", ann.session()).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activity.title").value("Activity one"));
        getAs("/api/v1/me/assignments", bo.session())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activity.title").value("Activity two"));
    }

    @Test
    void identityComesFromSessionNotFromRequest() throws Exception {
        MockHttpSession coordinator = coordinator();
        long first = createActivity(coordinator, "Activity one", "Topic", 2);
        long second = createActivity(coordinator, "Activity two", "Topic", 3);
        Candidate ann = activeCandidate(coordinator, "Ann Lee", "ann@example.test");
        Candidate bo = activeCandidate(coordinator, "Bo Park", "bo@example.test");
        assign(coordinator, ann.learnerId(), first);
        assign(coordinator, bo.learnerId(), second);

        // Ann adds Bo's id to the request in every way she can think of: still only her own training
        MvcResult result = getAs("/api/v1/me/assignments?learnerId=" + bo.learnerId() + "&id=" + bo.learnerId(),
                ann.session()).andExpect(status().isOk()).andReturn();
        JsonNode rows = body(result);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("learnerId").asLong()).isEqualTo(ann.learnerId());

        // and she cannot read Bo's assignments through the coordinator URL
        getAs("/api/v1/learners/" + bo.learnerId() + "/assignments", ann.session())
                .andExpect(status().isForbidden());
    }
}
