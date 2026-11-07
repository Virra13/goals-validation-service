package ru.virra.kkd.domain.metric;

import java.math.BigDecimal;

public record MetricValues(
        BigDecimal cleanGoalsShare,
        BigDecimal issuesPerGoal,
        BigDecimal goalsWithOwnerShare,
        BigDecimal goalsWithValidKrWeightsShare
) {
}
