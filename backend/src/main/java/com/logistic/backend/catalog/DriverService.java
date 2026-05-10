package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.DriverRequest;
import com.logistic.backend.api.dto.DriverResponse;
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
public class DriverService {

    private final DriverRepository driverRepository;
    private final PerformerRepository performerRepository;

    @Transactional
    public DriverResponse create(DriverRequest req, User current) {
        Performer p = loadPerformerForMutation(current, req.performerId());
        boolean firstForPerformer = driverRepository.countByEmployer_Id(p.getId()) == 0;
        boolean effectiveDefault = Boolean.TRUE.equals(req.isDefault()) || firstForPerformer;
        if (effectiveDefault) {
            driverRepository.clearDefaultForEmployer(p.getId());
            driverRepository.flush();
        }
        Driver d = new Driver();
        d.setEmployer(p);
        d.setFullName(req.fullName());
        d.setPhone(emptyToNull(req.phone()));
        d.setDefaultForEmployer(effectiveDefault);
        driverRepository.save(d);
        return toDto(d);
    }

    @Transactional
    public DriverResponse update(Long id, DriverRequest req, User current) {
        Driver d = loadDriver(current, id);
        Performer p = loadPerformerForMutation(current, req.performerId());
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
    public DriverResponse get(Long id, User current) {
        Driver d = loadDriver(current, id);
        return toDto(d);
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> list(String q, User current) {
        if (UserAccess.isAdmin(current)) {
            if (q == null || q.isBlank()) {
                return driverRepository.findAllByOrderByFullNameAsc().stream()
                        .map(this::toDto)
                        .toList();
            }
            return driverRepository.findByFullNameContainingIgnoreCaseOrderByFullNameAsc(q).stream()
                    .map(this::toDto)
                    .toList();
        }
        Long uid = current.getId();
        if (q == null || q.isBlank()) {
            return driverRepository.findByEmployer_Owner_IdOrderByFullNameAsc(uid).stream()
                    .map(this::toDto)
                    .toList();
        }
        return driverRepository
                .findByEmployer_Owner_IdAndFullNameContainingIgnoreCaseOrderByFullNameAsc(uid, q)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id, User current) {
        Driver d = loadDriver(current, id);
        driverRepository.delete(d);
    }

    private Performer loadPerformerForMutation(User current, Long performerId) {
        Performer p = performerRepository.findById(performerId).orElseThrow(this::notFound);
        if (!UserAccess.isAdmin(current) && !p.getOwner().getId().equals(current.getId())) {
            throw notFound();
        }
        return p;
    }

    private Driver loadDriver(User current, Long id) {
        if (UserAccess.isAdmin(current)) {
            return driverRepository.findById(id).orElseThrow(this::notFound);
        }
        return driverRepository.findByIdAndEmployer_Owner_Id(id, current.getId()).orElseThrow(this::notFound);
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
