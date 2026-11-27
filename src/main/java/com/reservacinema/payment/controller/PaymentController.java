package com.reservacinema.payment.controller;

import com.reservacinema.auth.security.AuthenticatedUser;
import com.reservacinema.payment.dto.PaymentRequest;
import com.reservacinema.payment.dto.PaymentResponse;
import com.reservacinema.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pagamento (simulado) de uma reserva. Sempre do usuário logado.
 */
@RestController
@RequestMapping("/api/reservations/{reservationId}")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payment")
    public PaymentResponse pay(@AuthenticationPrincipal AuthenticatedUser user,
                               @PathVariable Long reservationId,
                               @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(user.id(), reservationId, request);
    }

    @GetMapping("/payments")
    public List<PaymentResponse> history(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable Long reservationId) {
        return paymentService.history(user.id(), reservationId);
    }
}
