package com.logistic.backend.user;

import com.logistic.backend.api.dto.RegistrationRequestResponse;
import com.logistic.backend.api.dto.RegistrationStatusResponse;
import com.logistic.backend.api.dto.UserCreateRequest;
import com.logistic.backend.api.dto.UserResponse;
import java.util.HashSet;
import java.util.List;
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
    public UserResponse registerPending(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Такой логин уже занят");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setEnabled(false);
        u.setRegistrationStatus(RegistrationStatus.PENDING);
        u.setRoles(new HashSet<>(Set.of(Role.USER)));
        userRepository.save(u);
        return new UserResponse(u.getId(), u.getUsername(), u.getRoles());
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        validateRoles(request.roles());
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Такой логин уже занят");
        }
        User u = new User();
        u.setUsername(request.username());
        u.setPasswordHash(passwordEncoder.encode(request.password()));
        u.setEnabled(true);
        u.setRegistrationStatus(RegistrationStatus.APPROVED);
        u.setRoles(new HashSet<>(request.roles()));
        userRepository.save(u);
        return new UserResponse(u.getId(), u.getUsername(), u.getRoles());
    }

    @Transactional(readOnly = true)
    public RegistrationStatusResponse getRegistrationStatus(String username) {
        User u =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Пользователь не найден"));
        return new RegistrationStatusResponse(u.getUsername(), u.getRegistrationStatus());
    }

    @Transactional(readOnly = true)
    public List<RegistrationRequestResponse> listPendingRegistrations() {
        return userRepository
                .findByRegistrationStatusIn(
                        List.of(RegistrationStatus.PENDING, RegistrationStatus.REJECTED))
                .stream()
                .map(
                        u ->
                                new RegistrationRequestResponse(
                                        u.getId(), u.getUsername(), u.getRegistrationStatus()))
                .toList();
    }

    @Transactional
    public RegistrationRequestResponse approveRegistration(Long id) {
        User u = requireRegistrationRequest(id);
        u.setRegistrationStatus(RegistrationStatus.APPROVED);
        u.setEnabled(true);
        userRepository.save(u);
        return new RegistrationRequestResponse(u.getId(), u.getUsername(), u.getRegistrationStatus());
    }

    @Transactional
    public RegistrationRequestResponse rejectRegistration(Long id) {
        User u = requireRegistrationRequest(id);
        if (u.getRegistrationStatus() == RegistrationStatus.REJECTED) {
            return new RegistrationRequestResponse(u.getId(), u.getUsername(), u.getRegistrationStatus());
        }
        u.setRegistrationStatus(RegistrationStatus.REJECTED);
        u.setEnabled(false);
        userRepository.save(u);
        return new RegistrationRequestResponse(u.getId(), u.getUsername(), u.getRegistrationStatus());
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Текущий пароль указан неверно.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User requireRegistrationRequest(Long id) {
        User u =
                userRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Заявка не найдена"));
        if (u.getRegistrationStatus() == RegistrationStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Регистрация уже подтверждена");
        }
        return u;
    }

    private static void validateRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите хотя бы одну роль");
        }
        for (Role r : roles) {
            if (r != Role.USER && r != Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Недопустимая роль");
            }
        }
    }
}
