package ru.virra.kkd.domain.comparison;

import java.math.BigDecimal;

/**
 * Текущее и предыдущее значения метрики и результат их сравнения.
 * Дельта равна разности текущего и предыдущего значений. Если одно из них
 * отсутствует, дельта и направление изменения равны {@code null}.
 */
public record MetricChange(BigDecimal value, BigDecimal previous,
                           BigDecimal delta, Trend trend) {
}
