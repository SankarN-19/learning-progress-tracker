package com.example.lpt.service;

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
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {

    private final AssignmentRepository assignments;
    private final LearnerRepository learners;
    private final ActivityRepository activities;

    public AssignmentService(AssignmentRepository assignments, LearnerRepository learners,
                             ActivityRepository activities) {
        this.assignments = assignments;
        this.learners = learners;
        this.activities = activities;
    }

    @Transactional
    public AssignmentResponse assign(AssignmentRequest request) {
        Learner learner = learners.findById(request.learnerId())
                .orElseThrow(() -> ApiException.notFound("Learner not found."));
        Activity activity = activities.findById(request.activityId())
                .orElseThrow(() -> ApiException.notFound("Activity not found."));

        if (assignments.existsByLearnerIdAndActivityId(learner.getId(), activity.getId())) {
            throw ApiException.conflict("Assignment already exists.");
        }
        try {
            return AssignmentResponse.from(assignments.saveAndFlush(new Assignment(learner, activity)));
        } catch (DataIntegrityViolationException e) { // UNIQUE(learner_id, activity_id) as the safety net
            throw ApiException.conflict("Assignment already exists.");
        }
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listForLearner(Long learnerId) {
        if (!learners.existsById(learnerId)) {
            throw ApiException.notFound("Learner not found.");
        }
        return assignments.findByLearnerIdWithActivity(learnerId).stream().map(AssignmentResponse::from).toList();
    }

    /** Identity comes from the session (account id), never from a request parameter. */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> listMine(Long accountId) {
        Learner learner = learners.findByAccountId(accountId)
                .orElseThrow(() -> ApiException.notFound("Learner not found."));
        return assignments.findByLearnerIdWithActivity(learner.getId()).stream()
                .map(AssignmentResponse::from).toList();
    }

    /**
     * Order matters: 404 (no such assignment) -> 403 (not yours) -> 400 (bad status),
     * so a candidate learns nothing about other people's assignments.
     */
    @Transactional
    public AssignmentResponse updateStatus(Long assignmentId, Long callerAccountId, String statusLabel) {
        Assignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> ApiException.notFound("Assignment not found."));

        Learner caller = learners.findByAccountId(callerAccountId)
                .orElseThrow(() -> ApiException.forbidden("You can only update your own training."));
        if (!assignment.getLearner().getId().equals(caller.getId())) {
            throw ApiException.forbidden("You can only update your own training.");
        }

        assignment.setStatus(AssignmentStatus.fromLabel(statusLabel));
        return AssignmentResponse.from(assignment);
    }
}
