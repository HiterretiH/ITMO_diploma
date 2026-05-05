package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.PlaceRequest;
import com.logistic.backend.api.dto.PlaceResponse;
import com.logistic.backend.user.User;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    @Transactional
    public PlaceResponse create(User owner, PlaceRequest req) {
        Place p = new Place();
        p.setOwner(owner);
        apply(p, req);
        placeRepository.save(p);
        return toDto(p);
    }

    @Transactional
    public PlaceResponse update(User owner, Long id, PlaceRequest req) {
        Place p = placeRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, p);
        apply(p, req);
        placeRepository.save(p);
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public PlaceResponse get(User owner, Long id) {
        Place p = placeRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, p);
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public List<PlaceResponse> list(User owner, PlaceType typeFilter, String q) {
        Set<PlaceType> types = typesForFilter(typeFilter);
        List<Place> rows = placeRepository.findByOwnerAndPlaceTypeInOrderByAddressAsc(owner, types);
        if (q == null || q.isBlank()) {
            return rows.stream().map(this::toDto).toList();
        }
        String qq = q.trim().toLowerCase(Locale.ROOT);
        return rows.stream()
                .filter(
                        p -> {
                            if (p.getAddress().toLowerCase(Locale.ROOT).contains(qq)) {
                                return true;
                            }
                            return p.getContact() != null
                                    && p.getContact().toLowerCase(Locale.ROOT).contains(qq);
                        })
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(User owner, Long id) {
        Place p = placeRepository.findById(id).orElseThrow(this::notFound);
        assertOwner(owner, p);
        placeRepository.delete(p);
    }

    private static Set<PlaceType> typesForFilter(PlaceType typeFilter) {
        if (typeFilter == null) {
            return EnumSet.allOf(PlaceType.class);
        }
        return switch (typeFilter) {
            case LOAD -> EnumSet.of(PlaceType.LOAD, PlaceType.BOTH);
            case UNLOAD -> EnumSet.of(PlaceType.UNLOAD, PlaceType.BOTH);
            case BOTH -> EnumSet.of(PlaceType.BOTH);
        };
    }

    private void apply(Place p, PlaceRequest req) {
        p.setAddress(req.address().trim());
        p.setContact(emptyToNull(req.contact()));
        p.setPlaceType(req.placeType());
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static void assertOwner(User owner, Place p) {
        if (!p.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private PlaceResponse toDto(Place p) {
        return new PlaceResponse(p.getId(), p.getAddress(), p.getContact(), p.getPlaceType());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
