package com.reservacinema.movie.dto;

import com.reservacinema.movie.entity.Movie;

import java.util.List;

public record MovieResponse(
        Long id,
        String title,
        String description,
        int durationMinutes,
        String posterUrl,
        List<String> genres) {

    public static MovieResponse from(Movie movie) {
        // posterPath é o caminho interno (ex.: "posters/uuid.jpg"); viramos uma URL pública.
        String posterUrl = movie.getPosterPath() == null ? null : "/files/" + movie.getPosterPath();
        List<String> genres = movie.getGenres().stream().map(g -> g.getName()).sorted().toList();
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getDescription(),
                movie.getDurationMinutes(), posterUrl, genres);
    }
}
