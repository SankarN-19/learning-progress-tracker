package com.example.lpt.dto;

public record CohortSummaryResponse(long learners, long assignments, long completed,
                                    long inProgress, long notStarted) {
}
