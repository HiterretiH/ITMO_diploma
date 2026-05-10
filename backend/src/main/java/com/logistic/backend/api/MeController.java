package com.logistic.backend.api;

import com.logistic.backend.api.dto.TripFormDraftResponse;
import com.logistic.backend.order.TripFormDraftService;
import com.logistic.backend.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final TripFormDraftService tripFormDraftService;
    private final CurrentUserService currentUserService;

    @GetMapping("/trip-form-draft")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public TripFormDraftResponse tripFormDraft(@RequestParam(required = false) Long customerId) {
        return tripFormDraftService.getDraft(customerId, currentUserService.requireUser());
    }
}
