package ru.virra.kkd.controller;

import ru.virra.kkd.openapi.model.Severity;

import java.beans.PropertyEditorSupport;

/**
 * Преобразует строковый параметр {@code severity} в перечисление HTTP API.
 * Допустимые значения {@code critical}, {@code major} и {@code minor} заданы
 * контрактом OpenAPI; другие варианты написания не принимаются.
 */
final class SeverityQueryEditor extends PropertyEditorSupport {

    /**
     * Разбирает значение фильтра по важности проблемы.
     *
     * @param text строковое значение параметра {@code severity}
     * @throws IllegalArgumentException если значение не входит в перечисление OpenAPI
     */
    @Override
    public void setAsText(String text) {
        setValue(Severity.fromValue(text));
    }
}
