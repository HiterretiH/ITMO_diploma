package com.logistic.backend.api.dto;

import com.logistic.backend.user.RegistrationStatus;

public record RegisterResponse(String username, RegistrationStatus registrationStatus) {}
