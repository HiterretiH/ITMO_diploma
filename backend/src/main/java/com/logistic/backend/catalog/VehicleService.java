package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.VehicleRequest;
import com.logistic.backend.api.dto.VehicleResponse;
import com.logistic.backend.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public VehicleResponse create(User owner, VehicleRequest req) {
        Vehicle v = new Vehicle();
        v.setOwner(owner);
        apply(v, req);
        vehicleRepository.save(v);
        return toDto(v);
    }

    @Transactional
    public VehicleResponse update(User owner, Long id, VehicleRequest req) {
        Vehicle v = vehicleRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, v);
        apply(v, req);
        vehicleRepository.save(v);
        return toDto(v);
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(User owner, Long id) {
        Vehicle v = vehicleRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, v);
        return toDto(v);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> list(User owner, String q) {
        if (q == null || q.isBlank()) {
            return vehicleRepository.findByOwnerOrderByPlateNumberAsc(owner).stream()
                    .map(this::toDto)
                    .toList();
        }
        return vehicleRepository
                .findByOwnerAndPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(owner, q)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(User owner, Long id) {
        Vehicle v = vehicleRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, v);
        vehicleRepository.delete(v);
    }

    private void apply(Vehicle v, VehicleRequest req) {
        v.setPlateNumber(req.plateNumber());
        v.setModel(req.model());
        v.setLoadCapacityKg(req.loadCapacityKg());
    }

    private static void assertOwner(User owner, Vehicle v) {
        if (!v.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private VehicleResponse toDto(Vehicle v) {
        return new VehicleResponse(v.getId(), v.getPlateNumber(), v.getModel(), v.getLoadCapacityKg());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
