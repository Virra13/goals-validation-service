package ru.virra.kkd.repository.projection;

import ru.virra.kkd.domain.model.IssueObjectType;
import ru.virra.kkd.domain.model.IssueSeverity;

/**
 * Проекция проблемы для сводной статистики ККД.
 * Содержит уровень важности для подсчёта проблем по категориям и поля ключа
 * идентичности проблемы для сравнения прогонов; описание не загружается.
 */
public record IssueSummaryRow(long ruleId, IssueObjectType objectType, long objectId, IssueSeverity severity) {
}
