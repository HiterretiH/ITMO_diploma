package com.logistic.backend.bootstrap;

import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setEnabled(true);
        admin.setRoles(EnumSet.of(Role.ADMIN, Role.MANAGER, Role.EMPLOYEE));
        userRepository.save(admin);
    }
}
