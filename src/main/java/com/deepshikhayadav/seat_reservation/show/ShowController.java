package com.deepshikhayadav.seat_reservation.show;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}