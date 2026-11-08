package ru.virra.kkd.domain.metric;

import java.math.BigDecimal;

/**
 * Значения четырёх метрик ККД для выбранного набора активных целей.
 * При нулевом знаменателе отдельная метрика имеет значение {@code null}.
 * Доли выражаются в процентах; {@code issuesPerGoal} — среднее число проблем на цель.
 */
public record MetricValues(
        BigDecimal cleanGoalsShare,
        BigDecimal issuesPerGoal,
        BigDecimal goalsWithOwnerShare,
        BigDecimal goalsWithValidKrWeightsShare
) {
}
