package com.logistic.backend.security;

import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Unauthenticated");
        }
        return userRepository
                .findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
