package com.reservacinema.room.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    // Cada sala tem o seu próprio mapa de assentos.
    // cascade = ALL: salvar a sala também salva os assentos dela.
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rowLabel ASC, seatNumber ASC")
    private List<Seat> seats = new ArrayList<>();

    protected Room() {
    }

    public Room(String name) {
        this.name = name;
    }

    /** Adiciona um assento mantendo os dois lados da relação sincronizados. */
    public void addSeat(String rowLabel, int seatNumber) {
        seats.add(new Seat(this, rowLabel, seatNumber));
    }

    /** Capacidade da sala = quantidade de assentos no mapa. */
    public int getCapacity() {
        return seats.size();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public List<Seat> getSeats() { return seats; }

    public void setName(String name) { this.name = name; }
}
