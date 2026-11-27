package com.reservacinema.payment.service;

import com.reservacinema.common.exception.BusinessException;
import com.reservacinema.common.exception.NotFoundException;
import com.reservacinema.payment.dto.PaymentRequest;
import com.reservacinema.payment.dto.PaymentResponse;
import com.reservacinema.payment.entity.Payment;
import com.reservacinema.payment.entity.PaymentStatus;
import com.reservacinema.payment.repository.PaymentRepository;
import com.reservacinema.reservation.entity.Reservation;
import com.reservacinema.reservation.entity.ReservationStatus;
import com.reservacinema.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(ReservationRepository reservationRepository,
                              PaymentRepository paymentRepository) {
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    // noRollbackFor: um pagamento recusado lança exceção, mas o registro do pagamento recusado
    // deve PERMANECER no banco (histórico). Sem isso, a transação desfaria o insert.
    @Transactional(noRollbackFor = PaymentDeclinedException.class)
    public PaymentResponse pay(Long userId, Long reservationId, PaymentRequest request) {
        // Só o dono paga a própria reserva (mesma proteção contra IDOR da Etapa 4).
        Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new NotFoundException("Reserva " + reservationId + " não encontrada"));

        // Só faz sentido pagar uma reserva que está aguardando pagamento.
        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) {
            throw new BusinessException("Esta reserva não está aguardando pagamento (status: "
                    + reservation.getStatus() + ")");
        }
        // Não deixamos pagar uma reserva cuja sessão já começou.
        if (!reservation.getShowtime().getStartsAt().isAfter(Instant.now())) {
            throw new BusinessException("A sessão já começou; não é possível pagar esta reserva");
        }

        // ---- Simulação do gateway de pagamento ----
        // Regra combinada: cartão terminado em "0000" é recusado; o resto é aprovado.
        boolean approved = !request.cardNumber().endsWith("0000");

        Payment payment;
        if (approved) {
            String ref = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            payment = new Payment(reservation, reservation.getTotalAmount(),
                    PaymentStatus.APPROVED, request.method(), ref);
            reservation.confirm(); // a reserva passa a CONFIRMED
        } else {
            // Recusado: guardamos o registro, mas a reserva continua PENDING_PAYMENT
            // (o usuário pode tentar de novo com outro cartão).
            payment = new Payment(reservation, reservation.getTotalAmount(),
                    PaymentStatus.DECLINED, request.method(), null);
        }
        paymentRepository.save(payment);

        if (!approved) {
            // Lançar a exceção DEPOIS de salvar, sem desfazer o registro do pagamento recusado.
            // noRollbackFor (no @Transactional) garante que o insert do pagamento permaneça.
            throw new PaymentDeclinedException();
        }
        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> history(Long userId, Long reservationId) {
        // Confere que a reserva é do usuário antes de mostrar o histórico.
        reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new NotFoundException("Reserva " + reservationId + " não encontrada"));
        return paymentRepository.findByReservationIdOrderByCreatedAtDesc(reservationId).stream()
                .map(PaymentResponse::from)
                .toList();
    }
}
