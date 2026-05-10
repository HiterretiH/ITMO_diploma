package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.VehicleRequest;
import com.logistic.backend.api.dto.VehicleResponse;
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
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final PerformerRepository performerRepository;

    @Transactional
    public VehicleResponse create(VehicleRequest req, User current) {
        Performer p = loadPerformerForMutation(current, req.performerId());
        Vehicle v = new Vehicle();
        v.setOwner(p);
        v.setBrandModel(emptyToNull(req.brandModel()));
        v.setPlateNumber(emptyToNull(req.plateNumber()));
        v.setType(emptyToNull(req.type()));
        vehicleRepository.save(v);
        return toDto(v);
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest req, User current) {
        Vehicle v = loadVehicle(current, id);
        Performer p = loadPerformerForMutation(current, req.performerId());
        v.setOwner(p);
        v.setBrandModel(emptyToNull(req.brandModel()));
        v.setPlateNumber(emptyToNull(req.plateNumber()));
        v.setType(emptyToNull(req.type()));
        vehicleRepository.save(v);
        return toDto(v);
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(Long id, User current) {
        Vehicle v = loadVehicle(current, id);
        return toDto(v);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> list(String q, User current) {
        if (UserAccess.isAdmin(current)) {
            if (q == null || q.isBlank()) {
                return vehicleRepository.findAllByOrderByPlateNumberAsc().stream()
                        .map(this::toDto)
                        .toList();
            }
            return vehicleRepository.findByPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(q).stream()
                    .map(this::toDto)
                    .toList();
        }
        Long uid = current.getId();
        if (q == null || q.isBlank()) {
            return vehicleRepository.findByOwner_Owner_IdOrderByPlateNumberAsc(uid).stream()
                    .map(this::toDto)
                    .toList();
        }
        return vehicleRepository
                .findByOwner_Owner_IdAndPlateNumberContainingIgnoreCaseOrderByPlateNumberAsc(uid, q)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id, User current) {
        Vehicle v = loadVehicle(current, id);
        vehicleRepository.delete(v);
    }

    private Performer loadPerformerForMutation(User current, Long performerId) {
        Performer p = performerRepository.findById(performerId).orElseThrow(this::notFound);
        if (!UserAccess.isAdmin(current) && !p.getOwner().getId().equals(current.getId())) {
            throw notFound();
        }
        return p;
    }

    private Vehicle loadVehicle(User current, Long id) {
        if (UserAccess.isAdmin(current)) {
            return vehicleRepository.findById(id).orElseThrow(this::notFound);
        }
        return vehicleRepository.findByIdAndOwner_Owner_Id(id, current.getId()).orElseThrow(this::notFound);
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
                v.getType());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Транспорт не найден.");
    }
}
