package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.CustomerRequest;
import com.logistic.backend.api.dto.CustomerResponse;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserAccess;
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
    public CustomerResponse create(CustomerRequest req, User current) {
        Customer c = new Customer();
        c.setOwner(current);
        apply(c, req);
        customerRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req, User current) {
        Customer c = loadForMutation(current, id);
        apply(c, req);
        customerRepository.save(c);
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id, User current) {
        Customer c = loadForRead(current, id);
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(String q, User current) {
        if (UserAccess.isAdmin(current)) {
            if (q == null || q.isBlank()) {
                return customerRepository.findAllByOrderByShortNameAsc().stream()
                        .map(this::toDto)
                        .toList();
            }
            return customerRepository.findByShortNameContainingIgnoreCaseOrderByShortNameAsc(q).stream()
                    .map(this::toDto)
                    .toList();
        }
        Long uid = current.getId();
        if (q == null || q.isBlank()) {
            return customerRepository.findByOwner_IdOrderByShortNameAsc(uid).stream()
                    .map(this::toDto)
                    .toList();
        }
        return customerRepository.findByOwner_IdAndShortNameContainingIgnoreCaseOrderByShortNameAsc(uid, q)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id, User current) {
        Customer c = loadForMutation(current, id);
        customerRepository.delete(c);
    }

    private Customer loadForRead(User current, Long id) {
        if (UserAccess.isAdmin(current)) {
            return customerRepository.findById(id).orElseThrow(this::notFound);
        }
        return customerRepository.findByIdAndOwner_Id(id, current.getId()).orElseThrow(this::notFound);
    }

    private Customer loadForMutation(User current, Long id) {
        return loadForRead(current, id);
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
