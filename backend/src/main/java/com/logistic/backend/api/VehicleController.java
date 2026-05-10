package com.logistic.backend.api;

import com.logistic.backend.api.dto.VehicleRequest;
import com.logistic.backend.api.dto.VehicleResponse;
import com.logistic.backend.catalog.VehicleService;
import com.logistic.backend.security.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<VehicleResponse> list(@RequestParam(required = false) String q) {
        return vehicleService.list(q, currentUserService.requireUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public VehicleResponse get(@PathVariable Long id) {
        return vehicleService.get(id, currentUserService.requireUser());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public VehicleResponse create(@Valid @RequestBody VehicleRequest request) {
        return vehicleService.create(request, currentUserService.requireUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public VehicleResponse update(
            @PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(id, request, currentUserService.requireUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void delete(@PathVariable Long id) {
        vehicleService.delete(id, currentUserService.requireUser());
    }
}
