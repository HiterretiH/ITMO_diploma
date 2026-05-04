package com.logistic.backend.catalog;

import com.logistic.backend.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByOwnerAndFullNameContainingIgnoreCaseOrderByFullNameAsc(User owner, String q);

    List<Driver> findByOwnerOrderByFullNameAsc(User owner);
}
