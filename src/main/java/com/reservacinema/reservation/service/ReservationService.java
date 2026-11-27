package com.reservacinema.reservation.service;

import com.reservacinema.reservation.dto.CreateReservationRequest;
import com.reservacinema.reservation.dto.ReservationResponse;

import java.util.List;

public interface ReservationService {

    /** Cria uma reserva para o usuário logado, segurando os assentos escolhidos. */
    ReservationResponse create(Long userId, CreateReservationRequest request);

    /** Reservas do usuário logado, mais recentes primeiro. */
    List<ReservationResponse> listMine(Long userId);

    /** Uma reserva específica do usuário (404 se não for dele: ninguém vê a reserva de outro). */
    ReservationResponse getMine(Long userId, Long reservationId);

    /** Cancela uma reserva futura do usuário, liberando os assentos. */
    ReservationResponse cancel(Long userId, Long reservationId);
}
