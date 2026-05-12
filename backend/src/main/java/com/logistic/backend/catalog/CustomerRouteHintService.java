package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.CustomerRouteHintResponse;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserAccess;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerRouteHintService {

    private final CustomerRepository customerRepository;
    private final CustomerRouteHintRepository hintRepository;

    @Transactional(readOnly = true)
    public List<CustomerRouteHintResponse> list(User actor, long customerId, RouteHintKind kind, String q) {
        Customer c = loadForRead(actor, customerId);
        List<CustomerRouteHint> rows =
                hintRepository.findByCustomer_IdAndKindOrderByUpdatedAtDesc(c.getId(), kind);
        if (q == null || q.isBlank()) {
            return rows.stream().map(this::toDto).toList();
        }
        String needle = q.strip().toLowerCase(Locale.ROOT);
        return rows.stream()
                .filter(h -> h.getPlaceText().toLowerCase(Locale.ROOT).contains(needle))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void upsertFromOrder(Customer customer, RouteHintKind kind, String place, String contact) {
        if (place == null || place.isBlank()) {
            return;
        }
        Optional<String> keyOpt = RoutePlaceKeys.placeKeySha256(place);
        if (keyOpt.isEmpty()) {
            return;
        }
        String key = keyOpt.get();
        String displayPlace = place.strip();
        String contactNorm = contact == null || contact.isBlank() ? null : contact.strip();
        Instant now = Instant.now();
        Optional<CustomerRouteHint> existing =
                hintRepository.findByCustomer_IdAndKindAndPlaceKey(customer.getId(), kind, key);
        if (existing.isPresent()) {
            CustomerRouteHint h = existing.get();
            h.setPlaceText(displayPlace);
            h.setContactText(contactNorm);
            h.setUpdatedAt(now);
            hintRepository.save(h);
        } else {
            CustomerRouteHint h = new CustomerRouteHint();
            h.setCustomer(customer);
            h.setKind(kind);
            h.setPlaceKey(key);
            h.setPlaceText(displayPlace);
            h.setContactText(contactNorm);
            h.setUpdatedAt(now);
            hintRepository.save(h);
        }
    }

    private Customer loadForRead(User current, Long id) {
        if (UserAccess.isAdmin(current)) {
            return customerRepository.findById(id).orElseThrow(this::notFound);
        }
        return customerRepository.findByIdAndOwner_Id(id, current.getId()).orElseThrow(this::notFound);
    }

    private CustomerRouteHintResponse toDto(CustomerRouteHint h) {
        return new CustomerRouteHintResponse(
                h.getKind().name(), h.getPlaceText(), h.getContactText());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказчик не найден.");
    }
}
