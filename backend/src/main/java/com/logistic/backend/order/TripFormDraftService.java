package com.logistic.backend.order;

import com.logistic.backend.api.dto.TripFormDraftResponse;
import com.logistic.backend.catalog.Customer;
import com.logistic.backend.catalog.CustomerRepository;
import com.logistic.backend.catalog.Driver;
import com.logistic.backend.catalog.Performer;
import com.logistic.backend.catalog.Vehicle;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserAccess;
import com.logistic.backend.user.UserTripDefaults;
import com.logistic.backend.user.UserTripDefaultsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TripFormDraftService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final UserTripDefaultsRepository userTripDefaultsRepository;

    @Transactional(readOnly = true)
    public TripFormDraftResponse getDraft(Long customerId, User actor) {
        Long ownerId = resolveOwnerIdForNextOrderNumber(customerId, actor);
        int next = orderRepository.maxOrderNumberForOwner(ownerId) + 1;

        return userTripDefaultsRepository
                .findByUser_Id(actor.getId())
                .map(
                        d -> {
                            Long[] ids = sanitizeIds(d, actor);
                            return new TripFormDraftResponse(next, ids[0], ids[1], ids[2]);
                        })
                .orElse(new TripFormDraftResponse(next, null, null, null));
    }

    private Long resolveOwnerIdForNextOrderNumber(Long customerId, User actor) {
        if (customerId == null) {
            return actor.getId();
        }
        Customer c = customerRepository.findById(customerId).orElseThrow(this::notFound);
        assertCatalogRowAccessible(actor, c.getOwner().getId());
        return c.getOwner().getId();
    }

    private Long[] sanitizeIds(UserTripDefaults d, User actor) {
        Performer p = d.getLastPerformer();
        Long lastPid = null;
        if (p != null && catalogRowAccessible(actor, p.getOwner().getId())) {
            lastPid = p.getId();
        }

        Long lastDid = null;
        Driver dr = d.getLastDriver();
        if (lastPid != null && dr != null && catalogRowAccessible(actor, dr.getEmployer().getOwner().getId())) {
            if (dr.getEmployer().getId().equals(lastPid)) {
                lastDid = dr.getId();
            }
        }

        Long lastVid = null;
        Vehicle v = d.getLastVehicle();
        if (lastPid != null && v != null && catalogRowAccessible(actor, v.getOwner().getOwner().getId())) {
            if (v.getOwner().getId().equals(lastPid)) {
                lastVid = v.getId();
            }
        }

        return new Long[] {lastPid, lastDid, lastVid};
    }

    private void assertCatalogRowAccessible(User actor, Long dataOwnerId) {
        if (!catalogRowAccessible(actor, dataOwnerId)) {
            throw notFound();
        }
    }

    private static boolean catalogRowAccessible(User actor, Long dataOwnerId) {
        return UserAccess.isAdmin(actor) || actor.getId().equals(dataOwnerId);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена.");
    }
}
