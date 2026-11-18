package com.example.lpt.dto;

import jakarta.validation.constraints.NotNull;

public record AssignmentRequest(
        @NotNull(message = "Learner is required.") Long learnerId,
        @NotNull(message = "Activity is required.") Long activityId) {
}
