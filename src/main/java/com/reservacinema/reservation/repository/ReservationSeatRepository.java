package com.reservacinema.reservation.repository;

import com.reservacinema.reservation.entity.ReservationSeat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {

    // Assentos ocupados de uma sessão: usado para montar o mapa de disponibilidade.
    List<ReservationSeat> findByShowtimeIdAndActiveTrue(Long showtimeId);
}
