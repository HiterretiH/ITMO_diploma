package com.logistic.backend.api;

import com.logistic.backend.api.dto.CustomerRequest;
import com.logistic.backend.api.dto.CustomerResponse;
import com.logistic.backend.api.dto.CustomerPlaceResponse;
import com.logistic.backend.api.dto.CustomerPlaceSuggestionResponse;
import com.logistic.backend.catalog.CustomerPlaceKind;
import com.logistic.backend.catalog.CustomerPlaceService;
import com.logistic.backend.catalog.CustomerPlaceSuggestionService;
import com.logistic.backend.catalog.CustomerService;
import com.logistic.backend.security.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerPlaceService customerPlaceService;
    private final CustomerPlaceSuggestionService customerPlaceSuggestionService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<CustomerResponse> list(@RequestParam(required = false) String q) {
        return customerService.list(q, currentUserService.requireUser());
    }

    @GetMapping("/{id}/places")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<CustomerPlaceResponse> listPlaces(
            @PathVariable Long id,
            @RequestParam String kind,
            @RequestParam(required = false) String q) {
        CustomerPlaceKind k;
        try {
            k = CustomerPlaceKind.valueOf(kind.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind must be LOAD or UNLOAD");
        }
        return customerPlaceService.list(currentUserService.requireUser(), id, k, q);
    }

    @GetMapping("/{id}/place-suggestions")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<CustomerPlaceSuggestionResponse> placeSuggestions(
            @PathVariable Long id,
            @RequestParam String kind,
            @RequestParam(required = false) String q) {
        CustomerPlaceKind k;
        try {
            k = CustomerPlaceKind.valueOf(kind.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind must be LOAD or UNLOAD");
        }
        return customerPlaceSuggestionService.suggest(
                currentUserService.requireUser(), id, k, q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public CustomerResponse get(@PathVariable Long id) {
        return customerService.get(id, currentUserService.requireUser());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request, currentUserService.requireUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request, currentUserService.requireUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void delete(@PathVariable Long id) {
        customerService.delete(id, currentUserService.requireUser());
    }
}
