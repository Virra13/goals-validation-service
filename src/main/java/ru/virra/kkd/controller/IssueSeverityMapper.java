package ru.virra.kkd.controller;

import ru.virra.kkd.domain.model.IssueSeverity;
import ru.virra.kkd.openapi.model.Severity;

/**
 * Преобразует важность проблемы между перечислением HTTP API {@link Severity}
 * и перечислением внутренней модели {@link IssueSeverity}.
 * Значения HTTP API заданы контрактом OpenAPI в нижнем регистре.
 * Доменная логика и модели чтения используют собственный тип перечисления.
 */
final class IssueSeverityMapper {

    private IssueSeverityMapper() {
    }

    /**
     * Преобразует значение фильтра {@code severity} во внутренний тип.
     *
     * @param severity важность проблемы из HTTP-запроса или {@code null}, если фильтр отсутствует
     * @return соответствующее значение внутренней модели или {@code null}
     */
    static IssueSeverity toDomain(Severity severity) {
        if (severity == null) {
            return null;
        }
        return switch (severity) {
            case CRITICAL -> IssueSeverity.CRITICAL;
            case MAJOR -> IssueSeverity.MAJOR;
            case MINOR -> IssueSeverity.MINOR;
        };
    }

    /**
     * Преобразует важность сохранённой проблемы в тип ответа OpenAPI.
     *
     * @param severity важность проблемы во внутренней модели
     * @return соответствующее значение перечисления HTTP API
     */
    static Severity toApi(IssueSeverity severity) {
        return switch (severity) {
            case CRITICAL -> Severity.CRITICAL;
            case MAJOR -> Severity.MAJOR;
            case MINOR -> Severity.MINOR;
        };
    }
}
