package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.DriverRequest;
import com.logistic.backend.api.dto.DriverResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final PerformerRepository performerRepository;

    @Transactional
    public DriverResponse create(DriverRequest req) {
        Performer p = performerRepository.findById(req.performerId()).orElseThrow(this::notFound);
        if (Boolean.TRUE.equals(req.isDefault())) {
            driverRepository.clearDefaultForEmployer(p.getId());
            driverRepository.flush();
        }
        Driver d = new Driver();
        d.setEmployer(p);
        d.setFullName(req.fullName());
        d.setPhone(emptyToNull(req.phone()));
        d.setDefaultForEmployer(Boolean.TRUE.equals(req.isDefault()));
        driverRepository.save(d);
        return toDto(d);
    }

    @Transactional
    public DriverResponse update(Long id, DriverRequest req) {
        Driver d = driverRepository.findById(id).orElseThrow(this::notFound);
        Performer p = performerRepository.findById(req.performerId()).orElseThrow(this::notFound);
        if (Boolean.TRUE.equals(req.isDefault())) {
            driverRepository.clearDefaultForEmployer(p.getId());
            driverRepository.flush();
        }
        d.setEmployer(p);
        d.setFullName(req.fullName());
        d.setPhone(emptyToNull(req.phone()));
        d.setDefaultForEmployer(Boolean.TRUE.equals(req.isDefault()));
        driverRepository.save(d);
        return toDto(d);
    }

    @Transactional(readOnly = true)
    public DriverResponse get(Long id) {
        Driver d = driverRepository.findById(id).orElseThrow(this::notFound);
        return toDto(d);
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> list(String q) {
        if (q == null || q.isBlank()) {
            return driverRepository.findAllByOrderByFullNameAsc().stream()
                    .map(this::toDto)
                    .toList();
        }
        return driverRepository.findByFullNameContainingIgnoreCaseOrderByFullNameAsc(q).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Driver d = driverRepository.findById(id).orElseThrow(this::notFound);
        driverRepository.delete(d);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private DriverResponse toDto(Driver d) {
        return new DriverResponse(
                d.getId(),
                d.getEmployer().getId(),
                d.getFullName(),
                d.getPhone(),
                d.isDefaultForEmployer());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
