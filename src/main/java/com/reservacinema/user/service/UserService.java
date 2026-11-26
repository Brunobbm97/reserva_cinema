package com.reservacinema.user.service;

import com.reservacinema.user.dto.UserResponse;
import com.reservacinema.user.entity.Role;

import java.util.List;

public interface UserService {

    /** Dados do usuário logado. */
    UserResponse getById(Long id);

    /** Lista todos os usuários (uso do admin). */
    List<UserResponse> listAll();

    /** Muda o papel de um usuário (ex.: promover a ADMIN). */
    UserResponse changeRole(Long userId, Role newRole, Long requesterId);
}
