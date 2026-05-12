package com.logistic.backend.api.dto;

public record CustomerPlaceSuggestionResponse(
        PlaceSuggestionSource source, String kind, String address, String contact) {}
