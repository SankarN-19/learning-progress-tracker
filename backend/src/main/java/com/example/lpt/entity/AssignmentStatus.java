package com.example.lpt.entity;

import com.example.lpt.exception.ApiException;

public enum AssignmentStatus {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    private final String label;

    AssignmentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Parses the label used by the API ("Not Started", "In Progress", "Completed"). */
    public static AssignmentStatus fromLabel(String value) {
        if (value != null) {
            String trimmed = value.trim();
            for (AssignmentStatus s : values()) {
                if (s.label.equalsIgnoreCase(trimmed)) {
                    return s;
                }
            }
        }
        throw ApiException.badRequest("status", "Status must be Not Started, In Progress or Completed.");
    }
}
