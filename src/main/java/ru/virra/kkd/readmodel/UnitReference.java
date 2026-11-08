package ru.virra.kkd.readmodel;

/**
 * Ссылка на подразделение в модели чтения сводной статистики.
 * Содержит идентификатор и название выбранного подразделения.
 */
public record UnitReference(long id, String name) {
}
