package com.logistic.backend.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public final class ProblemDetailRu {

    private ProblemDetailRu() {}

    public static void setTitle(ProblemDetail pd, HttpStatusCode code) {
        int v = code.value();
        HttpStatus s = HttpStatus.resolve(v);
        if (s == null) {
            pd.setTitle("Ошибка " + v);
            return;
        }
        pd.setTitle(
                switch (s) {
                    case BAD_REQUEST -> "Некорректный запрос";
                    case UNAUTHORIZED -> "Требуется авторизация";
                    case FORBIDDEN -> "Доступ запрещён";
                    case NOT_FOUND -> "Не найдено";
                    case CONFLICT -> "Конфликт данных";
                    case UNPROCESSABLE_ENTITY -> "Ошибка проверки данных";
                    case INTERNAL_SERVER_ERROR -> "Ошибка сервера";
                    case BAD_GATEWAY -> "Ошибка шлюза";
                    case SERVICE_UNAVAILABLE -> "Сервис недоступен";
                    case GATEWAY_TIMEOUT -> "Превышено время ожидания";
                    default ->
                            s.getReasonPhrase() != null && !s.getReasonPhrase().isBlank()
                                    ? s.getReasonPhrase()
                                    : "Ошибка " + v;
                });
    }

    /** Fallback detail when ResponseStatusException has no reason. */
    public static String defaultDetailIfBlank(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Запись не найдена.";
            case BAD_REQUEST -> "Запрос не может быть выполнен.";
            case UNAUTHORIZED -> "Требуется авторизация.";
            case FORBIDDEN -> "Доступ запрещён.";
            case CONFLICT -> "Данные конфликтуют с уже существующими.";
            case INTERNAL_SERVER_ERROR ->
                    "На сервере произошла ошибка. Попробуйте позже или обратитесь к администратору.";
            default -> "Операция не выполнена (код " + status.value() + ").";
        };
    }
}
