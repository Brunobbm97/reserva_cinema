package com.reservacinema.payment.entity;

import com.reservacinema.reservation.entity.Reservation;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Registro de uma tentativa de pagamento (simulada) de uma reserva.
 * Guardamos tanto as aprovadas quanto as recusadas, para o histórico e os relatórios.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, length = 20)
    private String method;

    // Só preenchido quando aprovado: um código de comprovante fake.
    @Column(name = "transaction_ref", length = 40)
    private String transactionRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Payment() {
    }

    public Payment(Reservation reservation, BigDecimal amount, PaymentStatus status,
                   String method, String transactionRef) {
        this.reservation = reservation;
        this.amount = amount;
        this.status = status;
        this.method = method;
        this.transactionRef = transactionRef;
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }

    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getMethod() { return method; }
    public String getTransactionRef() { return transactionRef; }
    public Instant getCreatedAt() { return createdAt; }
}
