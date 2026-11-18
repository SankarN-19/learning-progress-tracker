package com.example.lpt.dto;

import com.example.lpt.entity.Activity;

public record ActivityResponse(Long id, String title, String topic, Double plannedHours) {

    public static ActivityResponse from(Activity a) {
        return new ActivityResponse(a.getId(), a.getTitle(), a.getTopic(), a.getPlannedHours());
    }
}
