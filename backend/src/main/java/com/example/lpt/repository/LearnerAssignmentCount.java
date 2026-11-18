package com.example.lpt.repository;

/** Projection: number of assignments per learner. */
public interface LearnerAssignmentCount {
    Long getLearnerId();

    Long getTotal();
}
