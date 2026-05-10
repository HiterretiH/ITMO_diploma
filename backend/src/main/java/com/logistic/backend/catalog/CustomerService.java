package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.CustomerRequest;
import com.logistic.backend.api.dto.CustomerResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        Customer c = new Customer();
        apply(c, req);
        customerRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req) {
        Customer c = customerRepository.findById(id).orElseThrow(this::notFound);
        apply(c, req);
        customerRepository.save(c);
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        Customer c = customerRepository.findById(id).orElseThrow(this::notFound);
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(String q) {
        if (q == null || q.isBlank()) {
            return customerRepository.findAllByOrderByShortNameAsc().stream()
                    .map(this::toDto)
                    .toList();
        }
        return customerRepository.findByShortNameContainingIgnoreCaseOrderByShortNameAsc(q).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Customer c = customerRepository.findById(id).orElseThrow(this::notFound);
        customerRepository.delete(c);
    }

    private void apply(Customer c, CustomerRequest req) {
        c.setShortName(req.shortName());
        c.setFullName(emptyToNull(req.fullName()));
        c.setPhone(emptyToNull(req.phone()));
        c.setRequisites(emptyToNull(req.requisites()));
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private CustomerResponse toDto(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getShortName(), c.getFullName(), c.getPhone(), c.getRequisites());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
