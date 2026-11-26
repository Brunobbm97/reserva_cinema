package com.reservacinema.reservation.entity;

import com.reservacinema.room.entity.Seat;
import com.reservacinema.showtime.entity.Showtime;
import com.reservacinema.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id")
    private Showtime showtime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING_PAYMENT;

    // Guardamos o total no momento da reserva: se o preço da sessão mudar depois,
    // o valor que o cliente pagou continua correto no histórico.
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationSeat> seats = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Reservation() {
    }

    public Reservation(User user, Showtime showtime) {
        this.user = user;
        this.showtime = showtime;
    }

    /** Adiciona um assento e recalcula o total (preço fixo da sessão x quantidade). */
    public void addSeat(Seat seat) {
        seats.add(new ReservationSeat(this, showtime, seat));
        totalAmount = showtime.getPrice().multiply(BigDecimal.valueOf(seats.size()));
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    /** Cancela a reserva e libera os assentos (active = false), mantendo o histórico. */
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        seats.forEach(ReservationSeat::release);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Showtime getShowtime() { return showtime; }
    public ReservationStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<ReservationSeat> getSeats() { return seats; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}
