package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.DriverRequest;
import com.logistic.backend.api.dto.DriverResponse;
import com.logistic.backend.user.User;
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

    @Transactional
    public DriverResponse create(User owner, DriverRequest req) {
        Driver d = new Driver();
        d.setOwner(owner);
        apply(d, req);
        driverRepository.save(d);
        return toDto(d);
    }

    @Transactional
    public DriverResponse update(User owner, Long id, DriverRequest req) {
        Driver d = driverRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, d);
        apply(d, req);
        return toDto(d);
    }

    @Transactional(readOnly = true)
    public DriverResponse get(User owner, Long id) {
        Driver d = driverRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, d);
        return toDto(d);
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> list(User owner, String q) {
        if (q == null || q.isBlank()) {
            return driverRepository.findByOwnerOrderByFullNameAsc(owner).stream()
                    .map(this::toDto)
                    .toList();
        }
        return driverRepository.findByOwnerAndFullNameContainingIgnoreCaseOrderByFullNameAsc(owner, q)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(User owner, Long id) {
        Driver d = driverRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, d);
        driverRepository.delete(d);
    }

    private void apply(Driver d, DriverRequest req) {
        d.setFullName(req.fullName());
        d.setLicenseNumber(req.licenseNumber());
        d.setLicenseCategory(req.licenseCategory());
    }

    private static void assertOwner(User owner, Driver d) {
        if (!d.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private DriverResponse toDto(Driver d) {
        return new DriverResponse(
                d.getId(), d.getFullName(), d.getLicenseNumber(), d.getLicenseCategory());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
