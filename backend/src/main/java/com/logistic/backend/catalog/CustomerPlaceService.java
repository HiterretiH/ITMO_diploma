package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.CustomerPlaceResponse;
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
public class CustomerPlaceService {

    private final CustomerRepository customerRepository;
    private final CustomerPlaceRepository placeRepository;

    @Transactional(readOnly = true)
    public List<CustomerPlaceResponse> list(
            User actor, long customerId, CustomerPlaceKind kind, String q) {
        Customer c = loadForRead(actor, customerId);
        List<CustomerPlace> rows =
                placeRepository.findByCustomer_IdAndKindOrderByUpdatedAtDesc(c.getId(), kind);
        if (q == null || q.isBlank()) {
            return rows.stream().map(this::toDto).toList();
        }
        String needle = q.strip().toLowerCase(Locale.ROOT);
        return rows.stream()
                .filter(p -> p.getAddressText().toLowerCase(Locale.ROOT).contains(needle))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void upsertFromOrder(
            Customer customer, CustomerPlaceKind kind, String address, String contact) {
        if (address == null || address.isBlank()) {
            return;
        }
        Optional<String> keyOpt = CustomerPlaceKeys.addressKeySha256(address);
        if (keyOpt.isEmpty()) {
            return;
        }
        String key = keyOpt.get();
        String displayAddress = address.strip();
        String contactNorm = contact == null || contact.isBlank() ? null : contact.strip();
        Instant now = Instant.now();
        Optional<CustomerPlace> existing =
                placeRepository.findByCustomer_IdAndKindAndAddressKey(
                        customer.getId(), kind, key);
        if (existing.isPresent()) {
            CustomerPlace p = existing.get();
            p.setAddressText(displayAddress);
            p.setContactText(contactNorm);
            p.setUpdatedAt(now);
            placeRepository.save(p);
        } else {
            CustomerPlace p = new CustomerPlace();
            p.setCustomer(customer);
            p.setKind(kind);
            p.setAddressKey(key);
            p.setAddressText(displayAddress);
            p.setContactText(contactNorm);
            p.setUpdatedAt(now);
            placeRepository.save(p);
        }
    }

    private Customer loadForRead(User current, Long id) {
        if (UserAccess.isAdmin(current)) {
            return customerRepository.findById(id).orElseThrow(this::notFound);
        }
        return customerRepository.findByIdAndOwner_Id(id, current.getId()).orElseThrow(this::notFound);
    }

    private CustomerPlaceResponse toDto(CustomerPlace p) {
        return new CustomerPlaceResponse(
                p.getKind().name(), p.getAddressText(), p.getContactText());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказчик не найден.");
    }
}
