package com.example.lpt;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/** FR-6: totals are calculated from saved data. */
class CohortSummaryApiTest extends AbstractApiTest {

    private static final String URL = "/api/v1/cohort-summary";

    @Test
    void emptyCohortHasZeroTotals() throws Exception {
        getAs(URL, coordinator()).andExpect(status().isOk())
                .andExpect(jsonPath("$.learners").value(0))
                .andExpect(jsonPath("$.assignments").value(0))
                .andExpect(jsonPath("$.completed").value(0));
    }

    @Test
    void totalsMatchSavedAssignments() throws Exception {
        MockHttpSession coordinator = coordinator();
        long first = createActivity(coordinator, "Activity one", "Topic", 2);
        long second = createActivity(coordinator, "Activity two", "Topic", 3);
        Candidate ann = activeCandidate(coordinator, "Ann Lee", "ann@example.test");
        Candidate bo = activeCandidate(coordinator, "Bo Park", "bo@example.test");

        long annFirst = assign(coordinator, ann.learnerId(), first);
        long annSecond = assign(coordinator, ann.learnerId(), second);
        assign(coordinator, bo.learnerId(), first);
        setStatus(ann, annFirst, "Completed");
        setStatus(ann, annSecond, "In Progress");

        getAs(URL, coordinator).andExpect(status().isOk())
                .andExpect(jsonPath("$.learners").value(2))
                .andExpect(jsonPath("$.assignments").value(3))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.inProgress").value(1))
                .andExpect(jsonPath("$.notStarted").value(1));
    }

    @Test
    void totalsFollowStatusChanges() throws Exception {
        MockHttpSession coordinator = coordinator();
        long activity = createActivity(coordinator, "Activity one", "Topic", 2);
        Candidate ann = activeCandidate(coordinator, "Ann Lee", "ann@example.test");
        long assignment = assign(coordinator, ann.learnerId(), activity);

        getAs(URL, coordinator).andExpect(jsonPath("$.completed").value(0));
        setStatus(ann, assignment, "Completed");
        getAs(URL, coordinator).andExpect(jsonPath("$.completed").value(1));
        setStatus(ann, assignment, "In Progress");
        getAs(URL, coordinator).andExpect(jsonPath("$.completed").value(0));
    }
}
