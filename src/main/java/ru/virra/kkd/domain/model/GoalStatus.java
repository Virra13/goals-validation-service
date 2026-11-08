package ru.virra.kkd.domain.model;

/**
 * Статус цели в источнике данных ККД.
 * Правила проверки и метрики прогонов учитывают только активные цели.
 */
public enum GoalStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    CANCELLED
}
