package com.logistic.backend.api.dto;

import com.logistic.backend.catalog.PlaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceRequest(
        @NotBlank @Size(max = 1024) String address,
        @Size(max = 512) String contact,
        @NotNull PlaceType placeType) {}
