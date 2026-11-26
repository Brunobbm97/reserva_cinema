package com.reservacinema.reservation.entity;

import com.reservacinema.room.entity.Seat;
import com.reservacinema.showtime.entity.Showtime;
import jakarta.persistence.*;

/**
 * Um assento dentro de uma reserva.
 * O banco tem um índice único em (showtime_id, seat_id) WHERE active:
 * é ele que impede que o mesmo assento seja vendido duas vezes.
 */
@Entity
@Table(name = "reservation_seats")
public class ReservationSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id")
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Column(nullable = false)
    private boolean active = true;

    protected ReservationSeat() {
    }

    ReservationSeat(Reservation reservation, Showtime showtime, Seat seat) {
        this.reservation = reservation;
        this.showtime = showtime;
        this.seat = seat;
    }

    void release() {
        this.active = false;
    }

    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public Showtime getShowtime() { return showtime; }
    public Seat getSeat() { return seat; }
    public boolean isActive() { return active; }
}
