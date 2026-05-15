package com.logistic.backend.api.dto;

import com.logistic.backend.user.RegistrationStatus;

public record RegistrationStatusResponse(String username, RegistrationStatus registrationStatus) {}
