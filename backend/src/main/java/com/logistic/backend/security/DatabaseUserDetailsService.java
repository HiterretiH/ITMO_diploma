package com.logistic.backend.security;

import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException(username));
        var authorities =
                u.getRoles().stream()
                        .map(Role::name)
                        .map(r -> "ROLE_" + r)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());
        return new org.springframework.security.core.userdetails.User(
                u.getUsername(), u.getPasswordHash(), u.isEnabled(), true, true, true, authorities);
    }
}
