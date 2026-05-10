package com.logistic.backend.api;

import com.logistic.backend.api.dto.DriverRequest;
import com.logistic.backend.api.dto.DriverResponse;
import com.logistic.backend.catalog.DriverService;
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
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<DriverResponse> list(@RequestParam(required = false) String q) {
        return driverService.list(q, currentUserService.requireUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public DriverResponse get(@PathVariable Long id) {
        return driverService.get(id, currentUserService.requireUser());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public DriverResponse create(@Valid @RequestBody DriverRequest request) {
        return driverService.create(request, currentUserService.requireUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public DriverResponse update(@PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        return driverService.update(id, request, currentUserService.requireUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void delete(@PathVariable Long id) {
        driverService.delete(id, currentUserService.requireUser());
    }
}
