package com.example.lpt.dto;

import com.example.lpt.entity.Assignment;

public record AssignmentResponse(Long id, Long learnerId, Long activityId, String status,
                                 ActivityResponse activity) {

    public static AssignmentResponse from(Assignment a) {
        return new AssignmentResponse(a.getId(), a.getLearner().getId(), a.getActivity().getId(),
                a.getStatus().label(), ActivityResponse.from(a.getActivity()));
    }
}
