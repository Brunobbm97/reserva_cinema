package com.reservacinema.user.controller;

import com.reservacinema.auth.security.AuthenticatedUser;
import com.reservacinema.user.dto.ChangeRoleRequest;
import com.reservacinema.user.dto.UserResponse;
import com.reservacinema.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Qualquer usuário logado: "quem sou eu?". O usuário vem do token, não de um parâmetro. */
    @GetMapping("/api/users/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getById(currentUser.id());
    }

    /** Só ADMIN (garantido pelo SecurityConfig, que protege /api/admin/**). */
    @GetMapping("/api/admin/users")
    public List<UserResponse> listUsers() {
        return userService.listAll();
    }

    /** Só ADMIN: promove ou rebaixa um usuário. */
    @PatchMapping("/api/admin/users/{id}/role")
    public UserResponse changeRole(@PathVariable Long id,
                                   @Valid @RequestBody ChangeRoleRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.changeRole(id, request.role(), currentUser.id());
    }
}
