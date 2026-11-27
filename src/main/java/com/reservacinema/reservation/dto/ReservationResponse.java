package com.reservacinema.reservation.dto;

import com.reservacinema.reservation.entity.Reservation;
import com.reservacinema.reservation.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReservationResponse(
        Long id,
        Long showtimeId,
        String movieTitle,
        String roomName,
        Instant showtimeStartsAt,
        ReservationStatus status,
        BigDecimal totalAmount,
        List<SeatItem> seats,
        Instant createdAt) {

    public record SeatItem(Long seatId, String label) {
    }

    public static ReservationResponse from(Reservation r) {
        // Mostramos só os assentos ativos (um cancelamento desativa, mas mantém o histórico).
        List<SeatItem> seats = r.getSeats().stream()
                .filter(rs -> rs.isActive())
                .map(rs -> new SeatItem(rs.getSeat().getId(), rs.getSeat().getLabel()))
                .toList();
        return new ReservationResponse(
                r.getId(),
                r.getShowtime().getId(),
                r.getShowtime().getMovie().getTitle(),
                r.getShowtime().getRoom().getName(),
                r.getShowtime().getStartsAt(),
                r.getStatus(),
                r.getTotalAmount(),
                seats,
                r.getCreatedAt());
    }
}
