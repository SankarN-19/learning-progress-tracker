package com.example.lpt.service;

import com.example.lpt.dto.CohortSummaryResponse;
import com.example.lpt.entity.AssignmentStatus;
import com.example.lpt.repository.AssignmentRepository;
import com.example.lpt.repository.LearnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SummaryService {

    private final LearnerRepository learners;
    private final AssignmentRepository assignments;

    public SummaryService(LearnerRepository learners, AssignmentRepository assignments) {
        this.learners = learners;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public CohortSummaryResponse summary() {
        return new CohortSummaryResponse(
                learners.count(),
                assignments.count(),
                assignments.countByStatus(AssignmentStatus.COMPLETED),
                assignments.countByStatus(AssignmentStatus.IN_PROGRESS),
                assignments.countByStatus(AssignmentStatus.NOT_STARTED));
    }
}
