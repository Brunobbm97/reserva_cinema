package com.reservacinema.payment.service;

import com.reservacinema.payment.dto.PaymentRequest;
import com.reservacinema.payment.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {

    /** Processa o pagamento (simulado) de uma reserva do usuário e, se aprovado, confirma a reserva. */
    PaymentResponse pay(Long userId, Long reservationId, PaymentRequest request);

    /** Histórico de tentativas de pagamento de uma reserva do usuário. */
    List<PaymentResponse> history(Long userId, Long reservationId);
}
