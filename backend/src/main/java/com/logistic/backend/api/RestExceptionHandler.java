package com.logistic.backend.api;

import com.logistic.backend.security.SecurityProblemMessages;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    private static final Map<String, String> FIELD_LABEL_RU =
            Map.ofEntries(
                    Map.entry("username", "Логин"),
                    Map.entry("password", "Пароль"),
                    Map.entry("customerId", "Заказчик"),
                    Map.entry("performerId", "Исполнитель"),
                    Map.entry("vehicleId", "Транспорт"),
                    Map.entry("driverId", "Водитель"),
                    Map.entry("roles", "Роли"));

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode code = ex.getStatusCode();
        HttpStatus resolved = HttpStatus.resolve(code.value());
        HttpStatus statusForDefaults =
                resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
        String detail = ex.getReason();
        if (detail == null || detail.isBlank()) {
            detail = ProblemDetailRu.defaultDetailIfBlank(statusForDefaults);
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code, detail);
        ProblemDetailRu.setTitle(pd, code);
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String msg =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(RestExceptionHandler::formatFieldError)
                        .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg);
        ProblemDetailRu.setTitle(pd, HttpStatus.BAD_REQUEST);
        List<Map<String, String>> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(RestExceptionHandler::fieldErrorToMap)
                        .toList();
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        String detail = SecurityProblemMessages.forbidden(ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
        ProblemDetailRu.setTitle(pd, HttpStatus.FORBIDDEN);
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        String detail = SecurityProblemMessages.unauthorized(ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        ProblemDetailRu.setTitle(pd, HttpStatus.UNAUTHORIZED);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Запрос содержит некорректный JSON.");
        ProblemDetailRu.setTitle(pd, HttpStatus.BAD_REQUEST);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String msg =
                ex.getConstraintViolations().stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg);
        ProblemDetailRu.setTitle(pd, HttpStatus.BAD_REQUEST);
        return pd;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex) {
        String detail =
                ex.getResourcePath() != null
                        ? ("Ресурс не найден: " + ex.getResourcePath())
                        : "Ресурс не найден.";
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail);
        ProblemDetailRu.setTitle(pd, HttpStatus.NOT_FOUND);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "На сервере произошла ошибка. Попробуйте позже или обратитесь к администратору.");
        ProblemDetailRu.setTitle(pd, HttpStatus.INTERNAL_SERVER_ERROR);
        return pd;
    }

    private static String formatFieldError(FieldError fe) {
        String label = FIELD_LABEL_RU.getOrDefault(fe.getField(), fe.getField());
        String message =
                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "ошибка проверки";
        return label + ": " + message;
    }

    private static Map<String, String> fieldErrorToMap(FieldError fe) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("field", fe.getField());
        m.put("message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "");
        return m;
    }
}
