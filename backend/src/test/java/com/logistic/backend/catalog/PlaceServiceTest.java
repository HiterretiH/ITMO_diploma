package com.logistic.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistic.backend.api.dto.PlaceRequest;
import com.logistic.backend.api.dto.PlaceResponse;
import com.logistic.backend.user.User;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock private PlaceRepository placeRepository;

    @InjectMocks private PlaceService placeService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(10L);
        owner.setUsername("emp");
    }

    @Test
    void listLoadFilterIncludesLoadAndBoth() {
        Place load = row(1L, "A", PlaceType.LOAD);
        Place both = row(2L, "B", PlaceType.BOTH);
        Place unload = row(3L, "C", PlaceType.UNLOAD);
        when(placeRepository.findByOwnerAndPlaceTypeInOrderByAddressAsc(
                        eq(owner), eq(EnumSet.of(PlaceType.LOAD, PlaceType.BOTH))))
                .thenReturn(List.of(load, both));

        List<PlaceResponse> out = placeService.list(owner, PlaceType.LOAD, null);

        assertThat(out).hasSize(2);
        assertThat(out.stream().map(PlaceResponse::placeType))
                .containsExactly(PlaceType.LOAD, PlaceType.BOTH);
    }

    @Test
    void listFiltersByQueryOnAddressAndContact() {
        Place p1 = row(1L, "Склад Москва", PlaceType.LOAD);
        p1.setContact("Иван");
        Place p2 = row(2L, "Другой адрес", PlaceType.LOAD);
        when(placeRepository.findByOwnerAndPlaceTypeInOrderByAddressAsc(any(), any()))
                .thenReturn(List.of(p1, p2));

        List<PlaceResponse> out = placeService.list(owner, PlaceType.LOAD, "москва");

        assertThat(out).hasSize(1);
        assertThat(out.get(0).address()).isEqualTo("Склад Москва");
    }

    @Test
    void createTrimsAddressAndNullsBlankContact() {
        when(placeRepository.save(any(Place.class)))
                .thenAnswer(
                        inv -> {
                            Place p = inv.getArgument(0);
                            p.setId(99L);
                            return p;
                        });

        placeService.create(
                owner,
                new PlaceRequest("  ул. Ленина 1  ", "  ", PlaceType.UNLOAD));

        ArgumentCaptor<Place> cap = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(cap.capture());
        Place saved = cap.getValue();
        assertThat(saved.getAddress()).isEqualTo("ул. Ленина 1");
        assertThat(saved.getContact()).isNull();
        assertThat(saved.getPlaceType()).isEqualTo(PlaceType.UNLOAD);
        assertThat(saved.getOwner()).isEqualTo(owner);
    }

    private Place row(long id, String address, PlaceType type) {
        Place p = new Place();
        p.setId(id);
        p.setOwner(owner);
        p.setAddress(address);
        p.setPlaceType(type);
        return p;
    }
}
