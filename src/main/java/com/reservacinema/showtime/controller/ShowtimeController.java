package com.reservacinema.showtime.controller;

import com.reservacinema.showtime.dto.SeatAvailabilityResponse;
import com.reservacinema.showtime.dto.ShowtimeRequest;
import com.reservacinema.showtime.dto.ShowtimeResponse;
import com.reservacinema.showtime.service.ShowtimeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    // ---- Leitura: pública ----

    /** Lista sessões. Com ?date=2026-12-01, filtra pelo dia; sem data, lista as futuras. */
    @GetMapping("/api/showtimes")
    public List<ShowtimeResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return showtimeService.listByDate(date);
    }

    @GetMapping("/api/showtimes/{id}")
    public ShowtimeResponse get(@PathVariable Long id) {
        return showtimeService.getById(id);
    }

    /** Mapa de assentos livres/ocupados desta sessão. */
    @GetMapping("/api/showtimes/{id}/seats")
    public SeatAvailabilityResponse seats(@PathVariable Long id) {
        return showtimeService.seatAvailability(id);
    }

    @GetMapping("/api/movies/{movieId}/showtimes")
    public List<ShowtimeResponse> byMovie(@PathVariable Long movieId) {
        return showtimeService.listByMovie(movieId);
    }

    // ---- Escrita: só ADMIN ----

    @PostMapping("/api/admin/showtimes")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowtimeResponse create(@Valid @RequestBody ShowtimeRequest request) {
        return showtimeService.create(request);
    }

    @DeleteMapping("/api/admin/showtimes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        showtimeService.delete(id);
    }
}
