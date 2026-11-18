package com.example.lpt.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** JSON error body: {"message": "...", "errors": {"field": "..."}} (errors is optional). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String message, Map<String, String> errors) {
}
