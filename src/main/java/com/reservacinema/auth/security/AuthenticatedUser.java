package com.reservacinema.auth.security;

import com.reservacinema.user.entity.Role;

/**
 * Quem está fazendo a requisição, extraído do access token.
 * É isso que os controllers recebem com @AuthenticationPrincipal.
 */
public record AuthenticatedUser(Long id, String email, Role role) {
}
