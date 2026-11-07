package ru.virra.kkd.domain.comparison;

import java.math.BigDecimal;

public record MetricChange(BigDecimal value, BigDecimal previous,
                           BigDecimal delta, Trend trend) {
}
