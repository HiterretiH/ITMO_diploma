package com.logistic.backend.api;

import com.logistic.backend.api.dto.JwtResponse;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.audit.AuditEventType;
import com.logistic.backend.audit.AuditService;
import com.logistic.backend.security.JwtService;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @PostMapping("/login")
    public JwtResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(), request.password()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        User u =
                userRepository
                        .findByUsername(request.username())
                        .orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid"));
        auditService.record(u, AuditEventType.LOGIN, Map.of("username", u.getUsername()));
        return new JwtResponse(jwtService.createToken(u));
    }
}
