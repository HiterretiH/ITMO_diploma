package com.logistic.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistic.backend.api.dto.CustomerPlaceSuggestionResponse;
import com.logistic.backend.api.dto.PlaceSuggestionSource;
import com.logistic.backend.typedata.PlaceSuggestionProperties;
import com.logistic.backend.typedata.TypeDataCachedSuggestService;
import com.logistic.backend.typedata.TypedataProperties;
import com.logistic.backend.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerPlaceSuggestionServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock CustomerPlaceRepository placeRepository;
    @Mock TypeDataCachedSuggestService typeDataCachedSuggestService;

    private final TypedataProperties typedataProperties = new TypedataProperties();
    private final PlaceSuggestionProperties placeSuggestionProperties = new PlaceSuggestionProperties();

    private CustomerPlaceSuggestionService service;

    private User owner;
    private Customer customer;

    @BeforeEach
    void setUp() {
        typedataProperties.setEnabled(false);
        typedataProperties.setToken("");
        service =
                new CustomerPlaceSuggestionService(
                        customerRepository,
                        placeRepository,
                        typeDataCachedSuggestService,
                        typedataProperties,
                        placeSuggestionProperties);
        owner = new User();
        owner.setId(42L);
        customer = new Customer();
        customer.setId(7L);
        customer.setOwner(owner);
    }

    @Test
    void mergesHistoryBeforeExternalAndSkipsDuplicateNormalizedAddresses() {
        typedataProperties.setEnabled(true);
        typedataProperties.setToken("secret");

        Instant t1 = Instant.parse("2026-01-02T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-03T00:00:00Z");
        CustomerPlace pOld = place(customer, CustomerPlaceKind.LOAD, "Shop  Beta", null, t1);
        CustomerPlace pNew = place(customer, CustomerPlaceKind.LOAD, "Warehouse  Alpha", "Alice", t2);

        when(customerRepository.findByIdAndOwner_Id(7L, 42L)).thenReturn(Optional.of(customer));
        when(placeRepository.findByCustomer_IdAndKindOrderByUpdatedAtDesc(7L, CustomerPlaceKind.LOAD))
                .thenReturn(List.of(pNew, pOld));

        when(typeDataCachedSuggestService.suggest("ware"))
                .thenReturn(List.of("Warehouse Gamma", "warehouse  alpha", "Other"));

        List<CustomerPlaceSuggestionResponse> res =
                service.suggest(owner, 7L, CustomerPlaceKind.LOAD, "ware");

        assertThat(res)
                .extracting(CustomerPlaceSuggestionResponse::source)
                .containsExactly(
                        PlaceSuggestionSource.HISTORY,
                        PlaceSuggestionSource.EXTERNAL,
                        PlaceSuggestionSource.EXTERNAL);
        assertThat(res.get(0).address()).isEqualTo("Warehouse  Alpha");
        assertThat(res.get(0).contact()).isEqualTo("Alice");
        assertThat(res.get(1).address()).isEqualTo("Warehouse Gamma");
        assertThat(res.get(2).address()).isEqualTo("Other");
    }

    @Test
    void doesNotCallTypedataWhenQueryBelowMinLength() {
        typedataProperties.setEnabled(true);
        typedataProperties.setToken("secret");
        CustomerPlace p =
                place(
                        customer,
                        CustomerPlaceKind.LOAD,
                        "Ab Street",
                        "x",
                        Instant.parse("2026-01-01T00:00:00Z"));
        when(customerRepository.findByIdAndOwner_Id(7L, 42L)).thenReturn(Optional.of(customer));
        when(placeRepository.findByCustomer_IdAndKindOrderByUpdatedAtDesc(7L, CustomerPlaceKind.LOAD))
                .thenReturn(List.of(p));

        service.suggest(owner, 7L, CustomerPlaceKind.LOAD, "a");

        verify(typeDataCachedSuggestService, never()).suggest(anyString());
    }

    @Test
    void doesNotCallTypedataWhenQueryTwoCharsBelowMinLength() {
        typedataProperties.setEnabled(true);
        typedataProperties.setToken("secret");
        CustomerPlace p =
                place(
                        customer,
                        CustomerPlaceKind.LOAD,
                        "Ab Street",
                        "x",
                        Instant.parse("2026-01-01T00:00:00Z"));
        when(customerRepository.findByIdAndOwner_Id(7L, 42L)).thenReturn(Optional.of(customer));
        when(placeRepository.findByCustomer_IdAndKindOrderByUpdatedAtDesc(7L, CustomerPlaceKind.LOAD))
                .thenReturn(List.of(p));

        service.suggest(owner, 7L, CustomerPlaceKind.LOAD, "ab");

        verify(typeDataCachedSuggestService, never()).suggest(anyString());
    }

    @Test
    void capsHistoryAtConfiguredMax() {
        placeSuggestionProperties.setMaxHistory(2);
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        CustomerPlace p3 = place(customer, CustomerPlaceKind.LOAD, "Addr 3", null, base.plusSeconds(3));
        CustomerPlace p2 = place(customer, CustomerPlaceKind.LOAD, "Addr 2", null, base.plusSeconds(2));
        CustomerPlace p1 = place(customer, CustomerPlaceKind.LOAD, "Addr 1", null, base.plusSeconds(1));
        CustomerPlace p0 = place(customer, CustomerPlaceKind.LOAD, "Addr 0", null, base);

        when(customerRepository.findByIdAndOwner_Id(7L, 42L)).thenReturn(Optional.of(customer));
        when(placeRepository.findByCustomer_IdAndKindOrderByUpdatedAtDesc(7L, CustomerPlaceKind.LOAD))
                .thenReturn(List.of(p3, p2, p1, p0));

        List<CustomerPlaceSuggestionResponse> res =
                service.suggest(owner, 7L, CustomerPlaceKind.LOAD, null);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).address()).isEqualTo("Addr 3");
        assertThat(res.get(1).address()).isEqualTo("Addr 2");
        assertThat(res).extracting(CustomerPlaceSuggestionResponse::source).containsOnly(PlaceSuggestionSource.HISTORY);
    }

    private static CustomerPlace place(
            Customer c, CustomerPlaceKind kind, String address, String contact, Instant updatedAt) {
        CustomerPlace p = new CustomerPlace();
        p.setCustomer(c);
        p.setKind(kind);
        p.setAddressKey("k-" + address.hashCode());
        p.setAddressText(address);
        p.setContactText(contact);
        p.setUpdatedAt(updatedAt);
        return p;
    }
}
