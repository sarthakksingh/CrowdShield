package com.crowdshield.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String path = ex.getBindingResult().getObjectName().toLowerCase();
        String errorType = "INVALID_REQUEST";
        if (path.contains("simulation")) {
            errorType = "INVALID_SIMULATION_REQUEST";
        } else if (path.contains("incident")) {
            errorType = "INVALID_INCIDENT_REPORT";
        } else if (path.contains("playback")) {
            errorType = "INVALID_PLAYBACK_REQUEST";
        }

        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> {
                    String message = err.getDefaultMessage();
                    return message == null ? err.getField() + " is invalid" : message;
                })
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "error", errorType,
                        "details", details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        String error = "INVALID_REQUEST";
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (message.contains("Zone not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "timestamp", Instant.now().toString(),
                            "error", "ZONE_NOT_FOUND",
                            "details", List.of(message)));
        } else if (message.contains("Scenario not found")) {
            error = "INVALID_PLAYBACK_REQUEST";
        } else if (message.contains("location.zoneId")) {
            error = "INVALID_INCIDENT_REPORT";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "error", error,
                        "details", List.of(message)));
    }
}

