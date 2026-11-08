package ru.virra.kkd.domain.comparison;

import ru.virra.kkd.domain.metric.MetricKey;
import ru.virra.kkd.domain.metric.MetricValues;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Сравнивает метрики и множества проблем текущего и предыдущего прогонов ККД.
 * Для метрик рассчитывает дельту и направление изменения; проблемы классифицирует
 * как новые, сохраняющиеся или исправленные по {@link IssueKey}.
 */
public final class RunComparator {

    /**
     * Сравнивает каждую метрику текущего прогона с соответствующей метрикой предыдущего.
     * Дельта равна текущему значению минус предыдущее. Направление изменения
     * определяется семантикой метрики: рост {@code issues_per_goal} означает ухудшение.
     *
     * @param current значения метрик текущего прогона
     * @param previous значения метрик предыдущего прогона или {@code null}, если сравнения нет
     * @return изменения по всем метрикам; при отсутствии сравнения поля {@code previous},
     *         {@code delta} и {@code trend} каждой метрики равны {@code null}
     */
    public Map<MetricKey, MetricChange> compareMetrics(
            MetricValues current, MetricValues previous) {
        Map<MetricKey, MetricChange> changes = new EnumMap<>(MetricKey.class);

        for (MetricKey key : MetricKey.values()) {
            BigDecimal value = key.valueOf(current);
            BigDecimal previousValue = previous == null ? null : key.valueOf(previous);
            changes.put(key, compareMetric(key, value, previousValue));
        }

        return Collections.unmodifiableMap(changes);
    }

    /**
     * Классифицирует проблемы по разности и пересечению множеств их ключей.
     *
     * @param current ключи проблем текущего прогона
     * @param previous ключи проблем предыдущего прогона или {@code null}, если сравнения нет
     * @return количество новых, сохраняющихся и исправленных проблем
     *         или {@code null} при отсутствии сравнения
     */
    public IssueDelta compareIssues(Set<IssueKey> current, Set<IssueKey> previous) {
        if (previous == null) {
            return null;
        }

        long persisting = 0;
        for (IssueKey issue : current) {
            if (previous.contains(issue)) {
                persisting++;
            }
        }
        return new IssueDelta(
                current.size() - persisting,
                persisting,
                previous.size() - persisting
        );
    }

    private MetricChange compareMetric(MetricKey key, BigDecimal value,
                                       BigDecimal previous) {
        if (value == null || previous == null) {
            return new MetricChange(value, previous, null, null);
        }

        BigDecimal delta = value.subtract(previous);
        int sign = delta.signum();
        if (sign == 0) {
            return new MetricChange(value, previous, delta, Trend.SAME);
        }

        boolean valueIncreased = sign > 0;
        boolean improved;
        if (key.higherIsBetter()) {
            improved = valueIncreased;
        } else {
            improved = !valueIncreased;
        }
        Trend trend = improved ? Trend.BETTER : Trend.WORSE;
        return new MetricChange(value, previous, delta, trend);
    }
}
