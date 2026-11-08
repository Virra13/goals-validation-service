package ru.virra.kkd.exception;

/**
 * Исключение для недопустимого параметра запроса ККД.
 * Обработчик HTTP API преобразует его в ответ 422 с кодом {@code INVALID_PARAMETER}.
 */
public class InvalidKkdParameterException extends RuntimeException {

    public InvalidKkdParameterException(String message) {
        super(message);
    }
}
