package com.logistic.backend.user;

import com.logistic.backend.api.dto.UserCreateRequest;
import com.logistic.backend.api.dto.UserResponse;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        validateRoles(request.roles());
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        User u = new User();
        u.setUsername(request.username());
        u.setPasswordHash(passwordEncoder.encode(request.password()));
        u.setEnabled(true);
        u.setRoles(new HashSet<>(request.roles()));
        userRepository.save(u);
        return new UserResponse(u.getId(), u.getUsername(), u.getRoles());
    }

    private static void validateRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role is required");
        }
        for (Role r : roles) {
            if (r != Role.USER && r != Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
            }
        }
    }
}
