package com.logistic.backend.catalog;

import com.logistic.backend.user.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByOwnerAndPlaceTypeInOrderByAddressAsc(User owner, Collection<PlaceType> types);
}
