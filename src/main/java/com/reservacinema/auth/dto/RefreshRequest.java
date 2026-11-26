package com.reservacinema.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Usado tanto para renovar os tokens quanto para fazer logout. */
public record RefreshRequest(@NotBlank String refreshToken) {
}
