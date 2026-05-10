package com.logistic.backend.api;

import com.logistic.backend.api.dto.PasswordChangeRequest;
import com.logistic.backend.api.dto.TripFormDraftResponse;
import com.logistic.backend.api.dto.UserResponse;
import com.logistic.backend.order.TripFormDraftService;
import com.logistic.backend.security.CurrentUserService;
import com.logistic.backend.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final TripFormDraftService tripFormDraftService;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public UserResponse me() {
        var u = currentUserService.requireUser();
        return new UserResponse(u.getId(), u.getUsername(), u.getRoles());
    }

    @PutMapping("/password")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(
                currentUserService.requireUser(),
                request.currentPassword(),
                request.newPassword());
    }

    @GetMapping("/trip-form-draft")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public TripFormDraftResponse tripFormDraft(@RequestParam(required = false) Long customerId) {
        return tripFormDraftService.getDraft(customerId, currentUserService.requireUser());
    }
}
