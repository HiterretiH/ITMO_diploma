package com.logistic.backend.bootstrap;

import com.logistic.backend.config.BootstrapAdminProperties;
import com.logistic.backend.user.RegistrationStatus;
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
import org.springframework.util.StringUtils;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties bootstrapAdminProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapAdminProperties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(bootstrapAdminProperties.getPassword())) {
            throw new IllegalStateException(
                    "app.bootstrap.admin.enabled=true requires a non-empty password "
                            + "(set BOOTSTRAP_ADMIN_PASSWORD or app.bootstrap.admin.password)");
        }
        if (!StringUtils.hasText(bootstrapAdminProperties.getUsername())) {
            throw new IllegalStateException(
                    "app.bootstrap.admin.enabled=true requires a non-empty username "
                            + "(set BOOTSTRAP_ADMIN_USERNAME or app.bootstrap.admin.username)");
        }
        if (userRepository.count() > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername(bootstrapAdminProperties.getUsername().trim());
        admin.setPasswordHash(passwordEncoder.encode(bootstrapAdminProperties.getPassword()));
        admin.setEnabled(true);
        admin.setRegistrationStatus(RegistrationStatus.APPROVED);
        admin.setRoles(EnumSet.of(Role.ADMIN));
        userRepository.save(admin);
    }
}
