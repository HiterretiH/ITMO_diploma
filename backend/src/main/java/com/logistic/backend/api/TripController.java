package com.logistic.backend.api;

import com.logistic.backend.api.dto.GeneratedDocumentResponse;
import com.logistic.backend.api.dto.TripResponse;
import com.logistic.backend.api.dto.TripUpdateRequest;
import com.logistic.backend.security.CurrentUserService;
import com.logistic.backend.trip.TripService;
import com.logistic.backend.trip.TripStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<TripResponse> list(@RequestParam(required = false) TripStatus status) {
        return tripService.list(currentUserService.requireUser(), status);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public TripResponse create() {
        return tripService.create(currentUserService.requireUser());
    }

    @GetMapping("/{id}")
    public TripResponse get(@PathVariable Long id) {
        return tripService.get(id, currentUserService.requireUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public TripResponse update(@PathVariable Long id, @RequestBody TripUpdateRequest request) {
        return tripService.update(id, request, currentUserService.requireUser());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public TripResponse submit(@PathVariable Long id) {
        return tripService.submit(id, currentUserService.requireUser());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public TripResponse approve(@PathVariable Long id) {
        return tripService.approve(id, currentUserService.requireUser());
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public TripResponse archive(@PathVariable Long id) {
        return tripService.archive(id, currentUserService.requireUser());
    }

    @GetMapping("/{id}/documents")
    public List<GeneratedDocumentResponse> listDocuments(@PathVariable Long id) {
        return tripService.listDocuments(id, currentUserService.requireUser());
    }
}
