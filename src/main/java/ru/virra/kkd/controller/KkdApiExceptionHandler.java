package ru.virra.kkd.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.virra.kkd.exception.InvalidKkdParameterException;
import ru.virra.kkd.exception.KkdResourceNotFoundException;
import ru.virra.kkd.openapi.model.ApiError;
import ru.virra.kkd.openapi.model.ApiErrorCode;
import ru.virra.kkd.openapi.model.ApiErrorResponse;
import ru.virra.kkd.openapi.model.Severity;

/**
 * Формирует единый ответ об ошибке HTTP API в соответствии с контрактом OpenAPI.
 * Запрос к отсутствующему прогону или подразделению возвращает код 404,
 * а некорректные параметры запроса — с кодом 422.
 * Тело ответа содержит объект {@code error} с кодом и сообщением.
 */
@RestControllerAdvice
public class KkdApiExceptionHandler {

    /**
     * Обрабатывает обращение к несуществующему прогону или подразделению.
     *
     * @param ex исключение с описанием отсутствующего ресурса
     * @return ошибка с кодом {@code RESOURCE_NOT_FOUND}
     */
    @ExceptionHandler(KkdResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse notFound(KkdResourceNotFoundException ex) {
        return new ApiErrorResponse(new ApiError(ApiErrorCode.RESOURCE_NOT_FOUND, ex.getMessage()));
    }

    /**
     * Обрабатывает параметр, не прошедший проверку контроллера или сервиса чтения.
     *
     * @param ex исключение с описанием неверного параметра
     * @return ошибка с кодом {@code INVALID_PARAMETER}
     */
    @ExceptionHandler(InvalidKkdParameterException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse invalidParameter(InvalidKkdParameterException ex) {
        return invalid(ex.getMessage());
    }

    /**
     * Обрабатывает отсутствие обязательного параметра HTTP-запроса.
     *
     * @param ex исключение Spring MVC с именем отсутствующего параметра
     * @return ошибка с кодом {@code INVALID_PARAMETER}
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse missingParameter(MissingServletRequestParameterException ex) {
        return invalid("Отсутствует обязательный параметр " + ex.getParameterName());
    }

    /**
     * Обрабатывает значение параметра, которое не удалось преобразовать в ожидаемый тип.
     * Для параметра {@code severity} возвращает сообщение с допустимыми значениями важности.
     *
     * @param ex исключение Spring MVC с именем параметра и ожидаемым типом
     * @return ошибка с кодом {@code INVALID_PARAMETER}
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse typeMismatch(MethodArgumentTypeMismatchException ex) {
        if (ex.getRequiredType() == Severity.class) {
            return invalid("severity должен быть critical, major или minor");
        }
        return invalid("Некорректное значение параметра " + ex.getName());
    }

    private static ApiErrorResponse invalid(String message) {
        return new ApiErrorResponse(new ApiError(ApiErrorCode.INVALID_PARAMETER, message));
    }
}
