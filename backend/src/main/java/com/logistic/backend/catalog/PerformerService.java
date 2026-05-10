package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.PerformerRequest;
import com.logistic.backend.api.dto.PerformerResponse;
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
public class PerformerService {

    private final PerformerRepository performerRepository;

    @Transactional
    public PerformerResponse create(PerformerRequest req, User current) {
        Performer p = new Performer();
        p.setOwner(current);
        apply(p, req);
        performerRepository.save(p);
        return toDto(p);
    }

    @Transactional
    public PerformerResponse update(Long id, PerformerRequest req, User current) {
        Performer p = loadForMutation(current, id);
        apply(p, req);
        performerRepository.save(p);
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public PerformerResponse get(Long id, User current) {
        Performer p = loadForRead(current, id);
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public List<PerformerResponse> list(String q, User current) {
        if (UserAccess.isAdmin(current)) {
            if (q == null || q.isBlank()) {
                return performerRepository.findAllByOrderByShortNameAsc().stream()
                        .map(this::toDto)
                        .toList();
            }
            return performerRepository.findByShortNameContainingIgnoreCaseOrderByShortNameAsc(q).stream()
                    .map(this::toDto)
                    .toList();
        }
        Long uid = current.getId();
        if (q == null || q.isBlank()) {
            return performerRepository.findByOwner_IdOrderByShortNameAsc(uid).stream()
                    .map(this::toDto)
                    .toList();
        }
        return performerRepository.findByOwner_IdAndShortNameContainingIgnoreCaseOrderByShortNameAsc(uid, q)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id, User current) {
        Performer p = loadForMutation(current, id);
        performerRepository.delete(p);
    }

    private Performer loadForRead(User current, Long id) {
        if (UserAccess.isAdmin(current)) {
            return performerRepository.findById(id).orElseThrow(this::notFound);
        }
        return performerRepository.findByIdAndOwner_Id(id, current.getId()).orElseThrow(this::notFound);
    }

    private Performer loadForMutation(User current, Long id) {
        return loadForRead(current, id);
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
