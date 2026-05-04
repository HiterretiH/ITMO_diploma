package com.logistic.backend.api.dto;

import com.logistic.backend.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotEmpty Set<Role> roles) {}
