package ru.virra.kkd.readmodel;

import ru.virra.kkd.domain.model.IssueSeverity;

/**
 * Количество проблем одного уровня важности в текущем и предыдущем прогонах.
 * Дельта равна разности текущего и предыдущего количества.
 * Если сравнения нет, {@code previous} и {@code delta} равны {@code null}.
 */
public record SeverityChange(IssueSeverity severity, long current, Long previous, Long delta) {
}
