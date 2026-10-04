package com.deepshikhayadav.seat_reservation.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------------------------------------
    // Business conflicts:
    //
    // 409 Conflict
    // ---------------------------------------------------------
    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<Map<String, Object>>
    handleReservationConflict(
            ReservationConflictException ex) {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put("error", ex.getCode());
        body.put("message", ex.getMessage());

        HttpStatus status = HttpStatus.CONFLICT;

        // Ownership failure should be 403 instead of 409.
        if ("FORBIDDEN".equals(ex.getCode())) {
            status = HttpStatus.FORBIDDEN;
        }

        return ResponseEntity
                .status(status)
                .body(body);
    }

    // ---------------------------------------------------------
    // Bad request.
    // ---------------------------------------------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>>
    handleBadRequest(
            IllegalArgumentException ex) {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put("error", "BAD_REQUEST");
        body.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    // ---------------------------------------------------------
    // Unexpected errors should still return 500.
    // ---------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleUnexpected(Exception ex) {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put("error", "INTERNAL_SERVER_ERROR");
        body.put("message", "An unexpected error occurred");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }
}