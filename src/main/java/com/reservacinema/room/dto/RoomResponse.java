package com.reservacinema.room.dto;

import com.reservacinema.room.entity.Room;

import java.util.List;

public record RoomResponse(Long id, String name, int capacity, List<SeatResponse> seats) {

    public static RoomResponse from(Room room) {
        List<SeatResponse> seats = room.getSeats().stream().map(SeatResponse::from).toList();
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity(), seats);
    }

    /** Versão enxuta, sem a lista de assentos (para listagens). */
    public static RoomResponse summary(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity(), List.of());
    }
}
