package com.reservacinema.user.dto;

import com.reservacinema.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {
}
