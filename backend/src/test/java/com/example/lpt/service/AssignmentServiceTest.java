package com.example.lpt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.lpt.dto.AssignmentRequest;
import com.example.lpt.dto.AssignmentResponse;
import com.example.lpt.entity.Activity;
import com.example.lpt.entity.Assignment;
import com.example.lpt.entity.AssignmentStatus;
import com.example.lpt.entity.Learner;
import com.example.lpt.exception.ApiException;
import com.example.lpt.repository.ActivityRepository;
import com.example.lpt.repository.AssignmentRepository;
import com.example.lpt.repository.LearnerRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock AssignmentRepository assignments;
    @Mock LearnerRepository learners;
    @Mock ActivityRepository activities;

    AssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AssignmentService(assignments, learners, activities);
    }

    private static Learner learner(long id) {
        Learner learner = new Learner("Learner " + id, "Cohort", null);
        ReflectionTestUtils.setField(learner, "id", id);
        return learner;
    }

    private static Activity activity(long id) {
        Activity activity = new Activity("Activity " + id, "Topic", 3.0);
        ReflectionTestUtils.setField(activity, "id", id);
        return activity;
    }

    private static void assertApiError(Throwable thrown, HttpStatus status, String message) {
        assertThat(thrown).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(status);
            assertThat(e.getMessage()).isEqualTo(message);
        });
    }

    @Test
    void assignCreatesNotStartedAssignment() {
        when(learners.findById(1L)).thenReturn(Optional.of(learner(1)));
        when(activities.findById(2L)).thenReturn(Optional.of(activity(2)));
        when(assignments.existsByLearnerIdAndActivityId(1L, 2L)).thenReturn(false);
        when(assignments.saveAndFlush(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignmentResponse response = service.assign(new AssignmentRequest(1L, 2L));

        assertThat(response.status()).isEqualTo("Not Started");
        assertThat(response.learnerId()).isEqualTo(1L);
        assertThat(response.activityId()).isEqualTo(2L);
    }

    @Test
    void assignRejectsDuplicateWithoutSaving() {
        when(learners.findById(1L)).thenReturn(Optional.of(learner(1)));
        when(activities.findById(2L)).thenReturn(Optional.of(activity(2)));
        when(assignments.existsByLearnerIdAndActivityId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.assign(new AssignmentRequest(1L, 2L)))
                .satisfies(e -> assertApiError(e, HttpStatus.CONFLICT, "Assignment already exists."));
        verify(assignments, never()).saveAndFlush(any());
    }

    @Test
    void assignTurnsDatabaseUniqueViolationIntoConflict() {
        when(learners.findById(1L)).thenReturn(Optional.of(learner(1)));
        when(activities.findById(2L)).thenReturn(Optional.of(activity(2)));
        when(assignments.existsByLearnerIdAndActivityId(1L, 2L)).thenReturn(false);
        when(assignments.saveAndFlush(any(Assignment.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.assign(new AssignmentRequest(1L, 2L)))
                .satisfies(e -> assertApiError(e, HttpStatus.CONFLICT, "Assignment already exists."));
    }

    @Test
    void assignRejectsUnknownLearner() {
        when(learners.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(new AssignmentRequest(1L, 2L)))
                .satisfies(e -> assertApiError(e, HttpStatus.NOT_FOUND, "Learner not found."));
        verifyNoInteractions(assignments);
    }

    @Test
    void assignRejectsUnknownActivity() {
        when(learners.findById(1L)).thenReturn(Optional.of(learner(1)));
        when(activities.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(new AssignmentRequest(1L, 2L)))
                .satisfies(e -> assertApiError(e, HttpStatus.NOT_FOUND, "Activity not found."));
    }

    @Test
    void ownerCanUpdateStatus() {
        Assignment assignment = new Assignment(learner(1), activity(5));
        when(assignments.findById(10L)).thenReturn(Optional.of(assignment));
        when(learners.findByAccountId(7L)).thenReturn(Optional.of(learner(1)));

        AssignmentResponse response = service.updateStatus(10L, 7L, "Completed");

        assertThat(response.status()).isEqualTo("Completed");
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
    }

    @Test
    void anotherLearnerCannotUpdateStatus() {
        Assignment assignment = new Assignment(learner(1), activity(5));
        when(assignments.findById(10L)).thenReturn(Optional.of(assignment));
        when(learners.findByAccountId(99L)).thenReturn(Optional.of(learner(2)));

        assertThatThrownBy(() -> service.updateStatus(10L, 99L, "Completed"))
                .satisfies(e -> assertApiError(e, HttpStatus.FORBIDDEN, "You can only update your own training."));
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.NOT_STARTED);
    }

    @Test
    void callerWithoutLearnerProfileIsForbidden() {
        Assignment assignment = new Assignment(learner(1), activity(5));
        when(assignments.findById(10L)).thenReturn(Optional.of(assignment));
        when(learners.findByAccountId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(10L, 1L, "Completed"))
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void invalidStatusIsBadRequestForTheOwner() {
        Assignment assignment = new Assignment(learner(1), activity(5));
        when(assignments.findById(10L)).thenReturn(Optional.of(assignment));
        when(learners.findByAccountId(7L)).thenReturn(Optional.of(learner(1)));

        assertThatThrownBy(() -> service.updateStatus(10L, 7L, "Done"))
                .satisfies(e -> assertApiError(e, HttpStatus.BAD_REQUEST,
                        "Status must be Not Started, In Progress or Completed."));
    }

    @Test
    void unknownAssignmentIsNotFoundBeforeAnyOwnershipCheck() {
        when(assignments.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(10L, 7L, "Completed"))
                .satisfies(e -> assertApiError(e, HttpStatus.NOT_FOUND, "Assignment not found."));
        verifyNoInteractions(learners);
    }
}
