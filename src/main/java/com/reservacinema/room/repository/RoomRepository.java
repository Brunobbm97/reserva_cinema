package com.reservacinema.room.repository;

import com.reservacinema.room.entity.Room;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
