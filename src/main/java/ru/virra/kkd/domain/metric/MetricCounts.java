package ru.virra.kkd.domain.metric;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Additive counts: ratios must be calculated only after subtree aggregation. */
public record MetricCounts(long activeGoals, long cleanGoals, long issues,
                           long goalsWithOwner, long goalsWithKeyResults, long goalsWithValidWeights) {
    public static final MetricCounts ZERO = new MetricCounts(0, 0, 0, 0, 0, 0);

    public MetricCounts plus(MetricCounts other) {
        return new MetricCounts(activeGoals + other.activeGoals, cleanGoals + other.cleanGoals,
                issues + other.issues, goalsWithOwner + other.goalsWithOwner,
                goalsWithKeyResults + other.goalsWithKeyResults,
                goalsWithValidWeights + other.goalsWithValidWeights);
    }

    public MetricValues values() {
        return new MetricValues(ratio(cleanGoals, activeGoals, 100), ratio(issues, activeGoals, 1),
                ratio(goalsWithOwner, activeGoals, 100),
                ratio(goalsWithValidWeights, goalsWithKeyResults, 100));
    }

    private static BigDecimal ratio(long numerator, long denominator, int multiplier) {
        return denominator == 0 ? null : BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(multiplier))
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
