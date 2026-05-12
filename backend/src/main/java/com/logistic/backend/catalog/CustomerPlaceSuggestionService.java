package com.logistic.backend.catalog;

import com.logistic.backend.api.dto.CustomerPlaceSuggestionResponse;
import com.logistic.backend.api.dto.PlaceSuggestionSource;
import com.logistic.backend.typedata.PlaceSuggestionProperties;
import com.logistic.backend.typedata.TypeDataCachedSuggestService;
import com.logistic.backend.typedata.TypedataProperties;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserAccess;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
public class CustomerPlaceSuggestionService {

    private final CustomerRepository customerRepository;
    private final CustomerPlaceRepository placeRepository;
    private final TypeDataCachedSuggestService typeDataCachedSuggestService;
    private final TypedataProperties typedataProperties;
    private final PlaceSuggestionProperties placeSuggestionProperties;

    @Transactional(readOnly = true)
    public List<CustomerPlaceSuggestionResponse> suggest(
            User actor, long customerId, CustomerPlaceKind kind, String q) {
        Customer c = loadForRead(actor, customerId);
        List<CustomerPlace> ordered =
                placeRepository.findByCustomer_IdAndKindOrderByUpdatedAtDesc(c.getId(), kind);
        String needle =
                q == null || q.isBlank() ? null : q.strip().toLowerCase(Locale.ROOT);
        List<CustomerPlace> historySlice = new ArrayList<>();
        for (CustomerPlace p : ordered) {
            if (historySlice.size() >= placeSuggestionProperties.getMaxHistory()) {
                break;
            }
            if (needle != null
                    && !p.getAddressText()
                            .toLowerCase(Locale.ROOT)
                            .contains(needle)) {
                continue;
            }
            historySlice.add(p);
        }
        Set<String> seenNorm = new LinkedHashSet<>();
        List<CustomerPlaceSuggestionResponse> out = new ArrayList<>();
        String kindName = kind.name();
        for (CustomerPlace p : historySlice) {
            String norm = CustomerPlaceKeys.normalizeForKey(p.getAddressText());
            if (!norm.isEmpty()) {
                seenNorm.add(norm);
            }
            out.add(
                    new CustomerPlaceSuggestionResponse(
                            PlaceSuggestionSource.HISTORY,
                            kindName,
                            p.getAddressText(),
                            p.getContactText()));
        }
        if (typedataProperties.isEnabled()
                && typedataProperties.hasToken()
                && needle != null
                && needle.length() >= placeSuggestionProperties.getExternalMinQueryLength()) {
            List<String> external =
                    typeDataCachedSuggestService.suggest(q.strip());
            int added = 0;
            for (String addr : external) {
                if (added >= placeSuggestionProperties.getMaxExternal()) {
                    break;
                }
                String norm = CustomerPlaceKeys.normalizeForKey(addr);
                if (norm.isEmpty() || seenNorm.contains(norm)) {
                    continue;
                }
                seenNorm.add(norm);
                out.add(
                        new CustomerPlaceSuggestionResponse(
                                PlaceSuggestionSource.EXTERNAL, kindName, addr, null));
                added++;
            }
        }
        return out;
    }

    private Customer loadForRead(User current, Long id) {
        if (UserAccess.isAdmin(current)) {
            return customerRepository.findById(id).orElseThrow(this::notFound);
        }
        return customerRepository.findByIdAndOwner_Id(id, current.getId()).orElseThrow(this::notFound);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказчик не найден.");
    }
}
