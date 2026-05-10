package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.VehicleRequest;
import com.logistic.backend.api.dto.VehicleResponse;
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
    private final PerformerRepository performerRepository;

    @Transactional
    public VehicleResponse create(VehicleRequest req) {
        Performer p = performerRepository.findById(req.performerId()).orElseThrow(this::notFound);
        if (Boolean.TRUE.equals(req.isDefault())) {
            vehicleRepository.clearDefaultForOwner(p.getId());
            vehicleRepository.flush();
        }
        Vehicle v = new Vehicle();
        v.setOwner(p);
        v.setBrandModel(emptyToNull(req.brandModel()));
        v.setPlateNumber(emptyToNull(req.plateNumber()));
        v.setType(emptyToNull(req.type()));
        v.setDefaultForPerformer(Boolean.TRUE.equals(req.isDefault()));
        vehicleRepository.save(v);
        return toDto(v);
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest req) {
        Vehicle v = vehicleRepository.findById(id).orElseThrow(this::notFound);
        Performer p = performerRepository.findById(req.performerId()).orElseThrow(this::notFound);
        if (Boolean.TRUE.equals(req.isDefault())) {
            vehicleRepository.clearDefaultForOwner(p.getId());
            vehicleRepository.flush();
        }
        v.setOwner(p);
        v.setBrandModel(emptyToNull(req.brandModel()));
        v.setPlateNumber(emptyToNull(req.plateNumber()));
        v.setType(emptyToNull(req.type()));
        v.setDefaultForPerformer(Boolean.TRUE.equals(req.isDefault()));
        vehicleRepository.save(v);
        return toDto(v);
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(Long id) {
        Vehicle v = vehicleRepository.findById(id).orElseThrow(this::notFound);
        return toDto(v);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> list(String q) {
        if (q == null || q.isBlank()) {
            return vehicleRepository.findAllByOrderByPlateNumberAsc().stream()
                    .map(this::toDto)
                    .toList();
        }
        return vehicleRepository.findByPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(q).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Vehicle v = vehicleRepository.findById(id).orElseThrow(this::notFound);
        vehicleRepository.delete(v);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private VehicleResponse toDto(Vehicle v) {
        return new VehicleResponse(
                v.getId(),
                v.getOwner().getId(),
                v.getBrandModel(),
                v.getPlateNumber(),
                v.getType(),
                v.isDefaultForPerformer());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
