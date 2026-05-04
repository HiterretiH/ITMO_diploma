package com.logistic.backend.user;

import com.logistic.backend.api.dto.UserCreateRequest;
import com.logistic.backend.api.dto.UserResponse;
import java.util.HashSet;
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
}
