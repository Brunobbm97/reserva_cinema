package com.reservacinema.showtime.dto;

import com.reservacinema.showtime.entity.Showtime;

import java.math.BigDecimal;
import java.time.Instant;

public record ShowtimeResponse(
        Long id,
        Long movieId,
        String movieTitle,
        Long roomId,
        String roomName,
        Instant startsAt,
        Instant endsAt,
        BigDecimal price) {

    public static ShowtimeResponse from(Showtime s) {
        return new ShowtimeResponse(
                s.getId(),
                s.getMovie().getId(), s.getMovie().getTitle(),
                s.getRoom().getId(), s.getRoom().getName(),
                s.getStartsAt(), s.getEndsAt(), s.getPrice());
    }
}
