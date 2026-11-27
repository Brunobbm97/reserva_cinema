package com.reservacinema.showtime.dto;

import java.util.List;

/**
 * Mapa de assentos de uma sessão, com o que está livre e o que já foi reservado.
 * O cliente usa isto para desenhar a sala e deixar escolher só os livres.
 */
public record SeatAvailabilityResponse(
        Long showtimeId,
        int totalSeats,
        int availableSeats,
        List<SeatStatus> seats) {

    public record SeatStatus(Long seatId, String label, String rowLabel, int seatNumber, boolean available) {
    }
}
