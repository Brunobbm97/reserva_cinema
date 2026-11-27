package com.reservacinema.payment.repository;

import com.reservacinema.payment.entity.Payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
}
