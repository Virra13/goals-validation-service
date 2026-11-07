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

@RestControllerAdvice
public class KkdApiExceptionHandler {

    @ExceptionHandler(KkdResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse notFound(KkdResourceNotFoundException ex) {
        return new ApiErrorResponse(new ApiError(ApiErrorCode.RESOURCE_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(InvalidKkdParameterException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse invalidParameter(InvalidKkdParameterException ex) {
        return invalid(ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse missingParameter(MissingServletRequestParameterException ex) {
        return invalid("Отсутствует обязательный параметр " + ex.getParameterName());
    }

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
