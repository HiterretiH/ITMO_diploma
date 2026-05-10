package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.PerformerRequest;
import com.logistic.backend.api.dto.PerformerResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PerformerService {

    private final PerformerRepository performerRepository;

    @Transactional
    public PerformerResponse create(PerformerRequest req) {
        Performer p = new Performer();
        apply(p, req);
        performerRepository.save(p);
        return toDto(p);
    }

    @Transactional
    public PerformerResponse update(Long id, PerformerRequest req) {
        Performer p = performerRepository.findById(id).orElseThrow(this::notFound);
        apply(p, req);
        performerRepository.save(p);
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public PerformerResponse get(Long id) {
        Performer p = performerRepository.findById(id).orElseThrow(this::notFound);
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public List<PerformerResponse> list(String q) {
        if (q == null || q.isBlank()) {
            return performerRepository.findAllByOrderByShortNameAsc().stream()
                    .map(this::toDto)
                    .toList();
        }
        return performerRepository.findByShortNameContainingIgnoreCaseOrderByShortNameAsc(q).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Performer p = performerRepository.findById(id).orElseThrow(this::notFound);
        performerRepository.delete(p);
    }

    private void apply(Performer p, PerformerRequest req) {
        p.setShortName(req.shortName());
        p.setFullName(emptyToNull(req.fullName()));
        p.setPhone(emptyToNull(req.phone()));
        p.setBankName(emptyToNull(req.bankName()));
        p.setInn(emptyToNull(req.inn()));
        p.setBik(emptyToNull(req.bik()));
        p.setKpp(emptyToNull(req.kpp()));
        p.setPaymentAccount(emptyToNull(req.paymentAccount()));
        p.setCorrAccount(emptyToNull(req.corrAccount()));
        p.setRequisites(emptyToNull(req.requisites()));
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private PerformerResponse toDto(Performer p) {
        return new PerformerResponse(
                p.getId(),
                p.getShortName(),
                p.getFullName(),
                p.getPhone(),
                p.getBankName(),
                p.getInn(),
                p.getBik(),
                p.getKpp(),
                p.getPaymentAccount(),
                p.getCorrAccount(),
                p.getRequisites());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
