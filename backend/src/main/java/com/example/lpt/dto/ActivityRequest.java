package com.example.lpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ActivityRequest(
        @NotBlank(message = "Title is required.")
        @Size(max = 150, message = "Title must be at most 150 characters.") String title,
        @NotBlank(message = "Topic is required.")
        @Size(max = 100, message = "Topic must be at most 100 characters.") String topic,
        @NotNull(message = "Planned hours is required.")
        @Positive(message = "Planned hours must be greater than zero.") Double plannedHours) {
}
