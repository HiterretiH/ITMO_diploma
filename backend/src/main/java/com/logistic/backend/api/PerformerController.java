package com.logistic.backend.api;

import com.logistic.backend.api.dto.PerformerRequest;
import com.logistic.backend.api.dto.PerformerResponse;
import com.logistic.backend.catalog.PerformerService;
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
@RequestMapping("/api/v1/performers")
@RequiredArgsConstructor
public class PerformerController {

    private final PerformerService performerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public List<PerformerResponse> list(@RequestParam(required = false) String q) {
        return performerService.list(q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public PerformerResponse get(@PathVariable Long id) {
        return performerService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public PerformerResponse create(@Valid @RequestBody PerformerRequest request) {
        return performerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public PerformerResponse update(
            @PathVariable Long id, @Valid @RequestBody PerformerRequest request) {
        return performerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public void delete(@PathVariable Long id) {
        performerService.delete(id);
    }
}
