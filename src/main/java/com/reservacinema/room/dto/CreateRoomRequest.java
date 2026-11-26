package com.reservacinema.room.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Dados para criar uma sala junto com o seu mapa de assentos.
 * Ex.: name="Sala 1", rows=[{label:"A", seats:10}, {label:"B", seats:10}]
 * cria a sala com os assentos A1..A10 e B1..B10.
 */
public record CreateRoomRequest(
        @NotBlank @Size(max = 60) String name,
        @NotEmpty @Valid List<RowDefinition> rows) {

    /** Uma fileira: o rótulo (A, B...) e quantos assentos ela tem. */
    public record RowDefinition(
            @NotBlank @Size(max = 5) String label,
            @Min(1) @Max(100) int seats) {
    }
}
