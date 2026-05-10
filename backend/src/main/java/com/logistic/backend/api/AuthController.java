package com.logistic.backend.api;

import com.logistic.backend.api.dto.JwtResponse;
import com.logistic.backend.api.dto.LoginRequest;
import com.logistic.backend.api.dto.RegisterRequest;
import com.logistic.backend.api.dto.UserCreateRequest;
import com.logistic.backend.audit.AuditEventType;
import com.logistic.backend.audit.AuditService;
import com.logistic.backend.security.JwtService;
import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import com.logistic.backend.user.UserRepository;
import com.logistic.backend.user.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuditService auditService;

    @PostMapping("/login")
    public JwtResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(), request.password()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
        }
        User u =
                userRepository
                        .findByUsername(request.username())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"));
        auditService.record(u, AuditEventType.LOGIN, Map.of("username", u.getUsername()));
        return new JwtResponse(jwtService.createToken(u));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public JwtResponse register(@Valid @RequestBody RegisterRequest request) {
        String username = request.username().trim();
        userService.create(
                new UserCreateRequest(username, request.password(), Set.of(Role.USER)));
        User u =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Не удалось завершить регистрацию (обратитесь к администратору)"));
        auditService.record(u, AuditEventType.REGISTER, Map.of("username", username));
        return new JwtResponse(jwtService.createToken(u));
    }
}
