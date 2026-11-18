package com.example.lpt.exception;

import org.springframework.http.HttpStatus;

/** One exception type for all expected API errors; the global handler maps it to JSON. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String field; // optional: lets the UI show the message inline

    private ApiException(HttpStatus status, String field, String message) {
        super(message);
        this.status = status;
        this.field = field;
    }

    public HttpStatus getStatus() { return status; }
    public String getField() { return field; }

    public static ApiException badRequest(String message) { return new ApiException(HttpStatus.BAD_REQUEST, null, message); }
    public static ApiException badRequest(String field, String message) { return new ApiException(HttpStatus.BAD_REQUEST, field, message); }
    public static ApiException unauthorized(String message) { return new ApiException(HttpStatus.UNAUTHORIZED, null, message); }
    public static ApiException forbidden(String message) { return new ApiException(HttpStatus.FORBIDDEN, null, message); }
    public static ApiException notFound(String message) { return new ApiException(HttpStatus.NOT_FOUND, null, message); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, null, message); }
    public static ApiException conflict(String field, String message) { return new ApiException(HttpStatus.CONFLICT, field, message); }
    public static ApiException tooManyRequests(String message) { return new ApiException(HttpStatus.TOO_MANY_REQUESTS, null, message); }
}
