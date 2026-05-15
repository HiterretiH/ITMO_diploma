package com.logistic.backend.api;

import com.logistic.backend.api.dto.RegistrationRequestResponse;
import com.logistic.backend.audit.AuditEventType;
import com.logistic.backend.audit.AuditService;
import com.logistic.backend.security.CurrentUserService;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import com.logistic.backend.user.UserService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/registration-requests")
@RequiredArgsConstructor
public class AdminRegistrationController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<RegistrationRequestResponse> list() {
        return userService.listPendingRegistrations();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public RegistrationRequestResponse approve(@PathVariable Long id) {
        RegistrationRequestResponse response = userService.approveRegistration(id);
        User target =
                userRepository
                        .findById(id)
                        .orElseThrow();
        User admin = currentUserService.requireUser();
        auditService.record(
                admin,
                AuditEventType.REGISTRATION_APPROVED,
                Map.of("targetUserId", id, "username", target.getUsername()));
        return response;
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public RegistrationRequestResponse reject(@PathVariable Long id) {
        RegistrationRequestResponse response = userService.rejectRegistration(id);
        User target =
                userRepository
                        .findById(id)
                        .orElseThrow();
        User admin = currentUserService.requireUser();
        auditService.record(
                admin,
                AuditEventType.REGISTRATION_REJECTED,
                Map.of("targetUserId", id, "username", target.getUsername()));
        return response;
    }
}
