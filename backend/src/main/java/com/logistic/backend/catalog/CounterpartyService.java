package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.CounterpartyRequest;
import com.logistic.backend.api.dto.CounterpartyResponse;
import com.logistic.backend.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CounterpartyService {

    private final CounterpartyRepository counterpartyRepository;

    @Transactional
    public CounterpartyResponse create(User owner, CounterpartyRequest req) {
        Counterparty c = new Counterparty();
        c.setOwner(owner);
        apply(c, req);
        counterpartyRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public CounterpartyResponse update(User owner, Long id, CounterpartyRequest req) {
        Counterparty c = counterpartyRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, c);
        apply(c, req);
        counterpartyRepository.save(c);
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public CounterpartyResponse get(User owner, Long id) {
        Counterparty c = counterpartyRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, c);
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public List<CounterpartyResponse> list(User owner, String q) {
        if (q == null || q.isBlank()) {
            return counterpartyRepository.findByOwnerOrderByNameAsc(owner).stream()
                    .map(this::toDto)
                    .toList();
        }
        return counterpartyRepository.findByOwnerAndNameContainingIgnoreCaseOrderByNameAsc(owner, q)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(User owner, Long id) {
        Counterparty c = counterpartyRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, c);
        counterpartyRepository.delete(c);
    }

    private void apply(Counterparty c, CounterpartyRequest req) {
        c.setName(req.name());
        c.setInn(emptyToNull(req.inn()));
        c.setLegalAddress(req.legalAddress());
        c.setPhone(req.phone());
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static void assertOwner(User owner, Counterparty c) {
        if (!c.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private CounterpartyResponse toDto(Counterparty c) {
        return new CounterpartyResponse(
                c.getId(), c.getName(), c.getInn(), c.getLegalAddress(), c.getPhone());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
