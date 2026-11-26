package com.reservacinema.showtime;

import com.reservacinema.movie.Movie;
import com.reservacinema.room.Room;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Uma sessão: um filme, em uma sala, em um horário, com preço fixo por assento. */
@Entity
@Table(name = "showtimes")
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    // Instant = um ponto exato no tempo (UTC). Evita confusão com fusos horários.
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    // BigDecimal para dinheiro: double tem erros de arredondamento (0.1 + 0.2 != 0.3).
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    protected Showtime() {
    }

    public Showtime(Movie movie, Room room, Instant startsAt, BigDecimal price) {
        this.movie = movie;
        this.room = room;
        this.startsAt = startsAt;
        this.endsAt = startsAt.plusSeconds(movie.getDurationMinutes() * 60L);
        this.price = price;
    }

    public Long getId() { return id; }
    public Movie getMovie() { return movie; }
    public Room getRoom() { return room; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public BigDecimal getPrice() { return price; }

    public void setPrice(BigDecimal price) { this.price = price; }
}
