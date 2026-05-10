package com.logistic.backend.api;

import com.logistic.backend.api.dto.CustomerRequest;
import com.logistic.backend.api.dto.CustomerResponse;
import com.logistic.backend.catalog.CustomerService;
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
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<CustomerResponse> list(@RequestParam(required = false) String q) {
        return customerService.list(q, currentUserService.requireUser());
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
