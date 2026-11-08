package ru.virra.kkd.readmodel;

import ru.virra.kkd.domain.comparison.IssueDelta;
import ru.virra.kkd.domain.comparison.MetricChange;
import ru.virra.kkd.domain.metric.MetricKey;

import java.util.List;
import java.util.Map;

/**
 * Модель чтения сводной статистики ККД по прогону и подразделению.
 * Содержит изменения метрик, разбивку проблем по важности и результат
 * сравнения множества проблем. Если прогон для сравнения не указан,
 * поля {@code comparedTo} и {@code delta} равны {@code null}.
 */
public record KkdSummary(
        RunReference run,
        RunReference comparedTo,
        UnitReference unit,
        Map<MetricKey, MetricChange> metrics,
        List<SeverityChange> issuesBySeverity,
        IssueDelta delta
) {
}
