package com.reservacinema.room.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: só carrega a sala do banco quando alguém realmente precisar dela.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "row_label", nullable = false, length = 5)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    protected Seat() {
    }

    Seat(Room room, String rowLabel, int seatNumber) {
        this.room = room;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
    }

    /** Nome amigável do assento, ex.: "C7". */
    public String getLabel() {
        return rowLabel + seatNumber;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public String getRowLabel() { return rowLabel; }
    public int getSeatNumber() { return seatNumber; }
}
