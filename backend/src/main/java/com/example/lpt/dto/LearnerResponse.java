package com.example.lpt.dto;

import com.example.lpt.entity.Learner;

/** assignedCount / completedCount feed the per-learner progress bar. No account internals. */
public record LearnerResponse(Long id, String displayName, String cohort, String email,
                              long assignedCount, long completedCount) {

    public static LearnerResponse from(Learner l, long assigned, long completed) {
        return new LearnerResponse(l.getId(), l.getDisplayName(), l.getCohort(),
                l.getAccount().getEmail(), assigned, completed);
    }
}
