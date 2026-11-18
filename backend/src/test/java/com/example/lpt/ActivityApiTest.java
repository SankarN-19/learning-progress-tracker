package com.example.lpt;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;

/** FR-1 and FR-2: show activities; coordinator creates and edits; all fields required; hours > 0. */
class ActivityApiTest extends AbstractApiTest {

    private static final String URL = "/api/v1/activities";

    @Test
    void listIsEmptyWhenNothingSaved() throws Exception {
        getAs(URL, coordinator()).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void coordinatorCreatesActivity() throws Exception {
        send(HttpMethod.POST, URL, coordinator(), Map.of("title", "React Basics", "topic", "Frontend", "plannedHours", 6))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("React Basics"))
                .andExpect(jsonPath("$.topic").value("Frontend"))
                .andExpect(jsonPath("$.plannedHours").value(6.0));
    }

    @Test
    void createTrimsTextFields() throws Exception {
        send(HttpMethod.POST, URL, coordinator(), Map.of("title", "  SQL  ", "topic", " Data ", "plannedHours", 4))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("SQL"))
                .andExpect(jsonPath("$.topic").value("Data"));
    }

    @Test
    void createRejectsMissingFields() throws Exception {
        MockHttpSession coordinator = coordinator();
        send(HttpMethod.POST, URL, coordinator, Map.of("topic", "Data", "plannedHours", 4))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.title").value("Title is required."));
        send(HttpMethod.POST, URL, coordinator, Map.of("title", "SQL", "plannedHours", 4))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.topic").value("Topic is required."));
        send(HttpMethod.POST, URL, coordinator, Map.of("title", "   ", "topic", "Data", "plannedHours", 4))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.title").exists());
        send(HttpMethod.POST, URL, coordinator, "{}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createRejectsHoursZeroOrNegative() throws Exception {
        MockHttpSession coordinator = coordinator();
        for (double hours : new double[] {0, -1, -2.5}) {
            send(HttpMethod.POST, URL, coordinator, Map.of("title", "SQL", "topic", "Data", "plannedHours", hours))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Planned hours must be greater than zero."))
                    .andExpect(jsonPath("$.errors.plannedHours").exists());
        }
    }

    @Test
    void createRejectsMissingOrNonNumericHours() throws Exception {
        MockHttpSession coordinator = coordinator();
        send(HttpMethod.POST, URL, coordinator, Map.of("title", "SQL", "topic", "Data"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.plannedHours").value("Planned hours is required."));
        send(HttpMethod.POST, URL, coordinator, Map.of("title", "SQL", "topic", "Data", "plannedHours", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or invalid."));
    }

    @Test
    void coordinatorEditsActivity() throws Exception {
        MockHttpSession coordinator = coordinator();
        long id = createActivity(coordinator, "Old title", "Old topic", 2);

        send(HttpMethod.PUT, URL + "/" + id, coordinator, Map.of("title", "New title", "topic", "New topic", "plannedHours", 9.5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.topic").value("New topic"))
                .andExpect(jsonPath("$.plannedHours").value(9.5));

        getAs(URL, coordinator).andExpect(jsonPath("$[0].title").value("New title"));
    }

    @Test
    void editRejectsInvalidData() throws Exception {
        MockHttpSession coordinator = coordinator();
        long id = createActivity(coordinator, "Keep me", "Topic", 2);

        send(HttpMethod.PUT, URL + "/" + id, coordinator, Map.of("title", "", "topic", "Topic", "plannedHours", 2))
                .andExpect(status().isBadRequest());
        send(HttpMethod.PUT, URL + "/" + id, coordinator, Map.of("title", "Keep me", "topic", "Topic", "plannedHours", 0))
                .andExpect(status().isBadRequest());

        getAs(URL, coordinator).andExpect(jsonPath("$[0].plannedHours").value(2.0)); // unchanged
    }

    @Test
    void editUnknownActivityIsNotFound() throws Exception {
        send(HttpMethod.PUT, URL + "/999999", coordinator(), Map.of("title", "X", "topic", "Y", "plannedHours", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Activity not found."));
    }

    @Test
    void deleteRemovesTheActivityAndItsAssignments() throws Exception {
        MockHttpSession coordinator = coordinator();
        long keep = createActivity(coordinator, "Keep me", "Topic", 2);
        long doomed = createActivity(coordinator, "Delete me", "Topic", 3);
        Candidate ann = activeCandidate(coordinator, "Ann Lee", "ann@example.test");
        assign(coordinator, ann.learnerId(), keep);
        assign(coordinator, ann.learnerId(), doomed);

        send(HttpMethod.DELETE, URL + "/" + doomed, coordinator, null).andExpect(status().isNoContent());

        getAs(URL, coordinator).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(keep));
        getAs("/api/v1/me/assignments", ann.session())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activity.id").value(keep));
        getAs("/api/v1/cohort-summary", coordinator).andExpect(jsonPath("$.assignments").value(1));
    }

    @Test
    void deleteUnknownActivityIsNotFound() throws Exception {
        send(HttpMethod.DELETE, URL + "/999999", coordinator(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Activity not found."));
    }

    @Test
    void savedActivitiesPersistAndAreListed() throws Exception {
        MockHttpSession coordinator = coordinator();
        createActivity(coordinator, "React Basics", "Frontend", 6);
        createActivity(coordinator, "REST APIs", "Backend", 8);

        // a brand-new login (like a page refresh) still sees the saved data
        getAs(URL, coordinator()).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("React Basics"))
                .andExpect(jsonPath("$[1].title").value("REST APIs"));
    }
}
