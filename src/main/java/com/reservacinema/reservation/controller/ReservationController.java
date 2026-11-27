package com.reservacinema.reservation.controller;

import com.reservacinema.auth.security.AuthenticatedUser;
import com.reservacinema.reservation.dto.CreateReservationRequest;
import com.reservacinema.reservation.dto.ReservationResponse;
import com.reservacinema.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Reservas do usuário logado. Em todas as rotas, o usuário vem do token
 * (@AuthenticationPrincipal), nunca de um parâmetro: assim ninguém reserva ou vê reserva alheia.
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody CreateReservationRequest request) {
        return reservationService.create(user.id(), request);
    }

    @GetMapping
    public List<ReservationResponse> listMine(@AuthenticationPrincipal AuthenticatedUser user) {
        return reservationService.listMine(user.id());
    }

    @GetMapping("/{id}")
    public ReservationResponse getMine(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long id) {
        return reservationService.getMine(user.id(), id);
    }

    /** Cancela a reserva (POST para deixar explícito que muda o estado). */
    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@AuthenticationPrincipal AuthenticatedUser user,
                                      @PathVariable Long id) {
        return reservationService.cancel(user.id(), id);
    }
}
