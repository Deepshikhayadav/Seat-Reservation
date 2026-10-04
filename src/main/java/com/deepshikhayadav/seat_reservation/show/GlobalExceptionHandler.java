package com.deepshikhayadav.seat_reservation.show;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.LinkedHashMap;
import java.util.Map;



@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<Map<String, Object>>
    handleReservationConflict(
            ReservationConflictException ex) {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put("error", ex.getCode());
        body.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(body);
    }
}