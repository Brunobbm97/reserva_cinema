package com.reservacinema.showtime.service;

import com.reservacinema.showtime.dto.SeatAvailabilityResponse;
import com.reservacinema.showtime.dto.ShowtimeRequest;
import com.reservacinema.showtime.dto.ShowtimeResponse;

import java.time.LocalDate;
import java.util.List;

public interface ShowtimeService {

    ShowtimeResponse create(ShowtimeRequest request);

    void delete(Long id);

    ShowtimeResponse getById(Long id);

    /** Sessões de um dia (no fuso informado). Se date for null, lista todas as futuras. */
    List<ShowtimeResponse> listByDate(LocalDate date);

    List<ShowtimeResponse> listByMovie(Long movieId);

    /** Mapa de assentos livres/ocupados de uma sessão. */
    SeatAvailabilityResponse seatAvailability(Long showtimeId);
}
