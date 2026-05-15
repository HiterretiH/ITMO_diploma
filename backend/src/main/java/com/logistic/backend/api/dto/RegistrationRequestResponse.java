package com.logistic.backend.api.dto;

import com.logistic.backend.user.RegistrationStatus;

public record RegistrationRequestResponse(
        Long id, String username, RegistrationStatus registrationStatus) {}
