package com.logistic.backend.api;

import com.logistic.backend.api.dto.CounterpartyRequest;
import com.logistic.backend.api.dto.CounterpartyResponse;
import com.logistic.backend.catalog.CounterpartyService;
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
@RequestMapping("/api/v1/counterparties")
@RequiredArgsConstructor
public class CounterpartyController {

    private final CounterpartyService counterpartyService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public List<CounterpartyResponse> list(@RequestParam(required = false) String q) {
        return counterpartyService.list(currentUserService.requireUser(), q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public CounterpartyResponse get(@PathVariable Long id) {
        return counterpartyService.get(currentUserService.requireUser(), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public CounterpartyResponse create(@Valid @RequestBody CounterpartyRequest request) {
        return counterpartyService.create(currentUserService.requireUser(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public CounterpartyResponse update(
            @PathVariable Long id, @Valid @RequestBody CounterpartyRequest request) {
        return counterpartyService.update(currentUserService.requireUser(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public void delete(@PathVariable Long id) {
        counterpartyService.delete(currentUserService.requireUser(), id);
    }
}
