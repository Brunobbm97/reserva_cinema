package com.reservacinema.room.dto;

import com.reservacinema.room.entity.Seat;

/** Um assento como a API mostra: id, fileira, número e o rótulo "C7". */
public record SeatResponse(Long id, String rowLabel, int seatNumber, String label) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(), seat.getLabel());
    }
}
