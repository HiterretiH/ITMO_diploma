package com.logistic.backend.api;

import com.logistic.backend.api.dto.PlaceRequest;
import com.logistic.backend.api.dto.PlaceResponse;
import com.logistic.backend.catalog.PlaceService;
import com.logistic.backend.catalog.PlaceType;
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
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public List<PlaceResponse> list(
            @RequestParam(required = false) PlaceType type,
            @RequestParam(required = false) String q) {
        return placeService.list(currentUserService.requireUser(), type, q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public PlaceResponse get(@PathVariable Long id) {
        return placeService.get(currentUserService.requireUser(), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public PlaceResponse create(@Valid @RequestBody PlaceRequest request) {
        return placeService.create(currentUserService.requireUser(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public PlaceResponse update(@PathVariable Long id, @Valid @RequestBody PlaceRequest request) {
        return placeService.update(currentUserService.requireUser(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public void delete(@PathVariable Long id) {
        placeService.delete(currentUserService.requireUser(), id);
    }
}
