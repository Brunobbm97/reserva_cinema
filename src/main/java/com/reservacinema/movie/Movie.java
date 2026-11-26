package com.reservacinema.movie;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // A duração é usada para calcular quando a sessão termina.
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    // Guardamos só o caminho do arquivo do pôster; o arquivo fica no disco.
    @Column(name = "poster_path")
    private String posterPath;

    // Relação N:N com gêneros, usando a tabela intermediária movie_genres.
    @ManyToMany
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Movie() {
    }

    public Movie(String title, String description, int durationMinutes) {
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getPosterPath() { return posterPath; }
    public Set<Genre> getGenres() { return genres; }
    public Instant getCreatedAt() { return createdAt; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public void setGenres(Set<Genre> genres) { this.genres = genres; }
}
