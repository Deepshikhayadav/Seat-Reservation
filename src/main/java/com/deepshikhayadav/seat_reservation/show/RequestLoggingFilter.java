package com.deepshikhayadav.seat_reservation.show;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter
        extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RequestLoggingFilter.class
            );

    private static final String REQUEST_ID =
            "request_id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // ---------------------------------------------------------
        // Reuse incoming X-Request-ID if provided.
        // Otherwise generate one.
        // ---------------------------------------------------------
        String requestId =
                request.getHeader("X-Request-ID");

        if (requestId == null ||
                requestId.isBlank()) {

            requestId =
                    UUID.randomUUID().toString();
        }

        long start =
                System.currentTimeMillis();

        try {

            // -----------------------------------------------------
            // Put request ID into MDC.
            //
            // Logback automatically includes this in JSON logs.
            // -----------------------------------------------------
            MDC.put(
                    REQUEST_ID,
                    requestId
            );

            // Return the ID to the caller.
            response.setHeader(
                    "X-Request-ID",
                    requestId
            );

            filterChain.doFilter(
                    request,
                    response
            );

        } finally {

            long duration =
                    System.currentTimeMillis()
                            - start;

            // -----------------------------------------------------
            // Structured request log.
            // -----------------------------------------------------
            log.info(
                    "HTTP request completed: method={} path={} status={} duration_ms={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration
            );

            MDC.remove(REQUEST_ID);
        }
    }
}