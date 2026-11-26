package com.reservacinema.user.dto;

import com.reservacinema.user.entity.Role;
import com.reservacinema.user.entity.User;

import java.time.Instant;

/**
 * O que a API mostra de um usuário. Nunca devolvemos a entidade User direto,
 * senão o hash da senha iria junto na resposta.
 */
public record UserResponse(Long id, String name, String email, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
