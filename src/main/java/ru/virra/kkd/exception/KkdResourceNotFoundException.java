package ru.virra.kkd.exception;

/**
 * Исключение для отсутствующего прогона ККД или подразделения.
 * Обработчик HTTP API преобразует его в ответ 404 с кодом {@code RESOURCE_NOT_FOUND}.
 */
public class KkdResourceNotFoundException extends RuntimeException {

    public KkdResourceNotFoundException(String message) {
        super(message);
    }
}
