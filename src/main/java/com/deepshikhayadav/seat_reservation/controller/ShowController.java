package com.deepshikhayadav.seat_reservation.controller;


import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.deepshikhayadav.seat_reservation.model.CreateShowRequest;
import com.deepshikhayadav.seat_reservation.model.Show;
import com.deepshikhayadav.seat_reservation.model.ShowResponse;
import com.deepshikhayadav.seat_reservation.services.ShowService;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Show createShow(
            @Valid @RequestBody CreateShowRequest request) {

        return showService.createShow(request);
    }

    @GetMapping("/{showId}")
    public ShowResponse getShow(@PathVariable UUID showId) {
        return showService.getShow(showId);
    }
}