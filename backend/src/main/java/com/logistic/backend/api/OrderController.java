package com.logistic.backend.api;

import com.logistic.backend.api.dto.GeneratedDocumentResponse;
import com.logistic.backend.api.dto.OrderCreateRequest;
import com.logistic.backend.api.dto.OrderResponse;
import com.logistic.backend.api.dto.OrderUpdateRequest;
import com.logistic.backend.order.OrderService;
import com.logistic.backend.security.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<OrderResponse> list() {
        return orderService.list(currentUserService.requireUser());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.create(request, currentUserService.requireUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.get(id, currentUserService.requireUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public OrderResponse update(@PathVariable Long id, @RequestBody OrderUpdateRequest request) {
        return orderService.update(id, request, currentUserService.requireUser());
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public OrderResponse complete(@PathVariable Long id) {
        return orderService.complete(id, currentUserService.requireUser());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void delete(@PathVariable Long id) {
        orderService.delete(id, currentUserService.requireUser());
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<GeneratedDocumentResponse> listDocuments(@PathVariable Long id) {
        return orderService.listDocuments(id, currentUserService.requireUser());
    }
}
