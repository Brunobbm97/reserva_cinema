package com.reservacinema.movie.dto;

import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Dados para criar ou atualizar um filme.
 * genres são nomes (ex.: ["Ação", "Aventura"]); o service cria os que ainda não existirem.
 */
public record MovieRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @Min(1) @Max(1000) int durationMinutes,
        List<@NotBlank @Size(max = 60) String> genres) {

    public List<String> genresOrEmpty() {
        return genres == null ? List.of() : genres;
    }
}
