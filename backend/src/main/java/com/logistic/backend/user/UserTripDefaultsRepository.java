package com.logistic.backend.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTripDefaultsRepository extends JpaRepository<UserTripDefaults, Long> {

    Optional<UserTripDefaults> findByUser_Id(Long userId);
}
