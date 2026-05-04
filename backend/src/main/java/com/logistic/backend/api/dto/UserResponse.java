package com.logistic.backend.api.dto;

import com.logistic.backend.user.Role;
import java.util.Set;

public record UserResponse(Long id, String username, Set<Role> roles) {}
