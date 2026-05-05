package com.logistic.backend.api.dto;

import com.logistic.backend.catalog.PlaceType;

public record PlaceResponse(Long id, String address, String contact, PlaceType placeType) {}
