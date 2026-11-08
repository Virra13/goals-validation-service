package ru.virra.kkd.controller;

import java.beans.PropertyEditorSupport;

/**
 * Проверяет формат числовых идентификаторов в параметрах HTTP-запроса.
 * Переданное значение должно состоять только из десятичных цифр; пустые,
 * повторённые и содержащие иные символы значения отклоняются при привязке.
 * Отсутствующий необязательный параметр остаётся {@code null}.
 */
final class StrictLongQueryEditor extends PropertyEditorSupport {

    /**
     * Преобразует переданный идентификатор в {@link Long}.
     * Проверка положительного значения выполняется контроллером после привязки.
     *
     * @param text строковое значение идентификатора
     * @throws IllegalArgumentException если формат неверен или число не помещается в {@link Long}
     */
    @Override
    public void setAsText(String text) {
        if (text == null || !text.matches("[0-9]+")) {
            throw new IllegalArgumentException("Ожидается один целочисленный ID");
        }
        setValue(Long.valueOf(text));
    }
}
