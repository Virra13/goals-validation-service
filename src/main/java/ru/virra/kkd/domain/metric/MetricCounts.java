package ru.virra.kkd.domain.metric;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Счётчики, из которых рассчитываются метрики ККД.
 * Счётчики подразделений складываются по поддереву до вычисления долей,
 * чтобы итоговые показатели не зависели от размера отдельных подразделений.
 */
public record MetricCounts(long activeGoals, long cleanGoals, long issues,
                           long goalsWithOwner, long goalsWithKeyResults, long goalsWithValidWeights) {
    /**
     * Складывает счётчики без вычисления и округления промежуточных значений метрик.
     *
     * @param other счётчики добавляемого подразделения
     * @return сумма соответствующих счётчиков
     */
    public MetricCounts plus(MetricCounts other) {
        return new MetricCounts(activeGoals + other.activeGoals, cleanGoals + other.cleanGoals,
                issues + other.issues, goalsWithOwner + other.goalsWithOwner,
                goalsWithKeyResults + other.goalsWithKeyResults,
                goalsWithValidWeights + other.goalsWithValidWeights);
    }

    /**
     * Рассчитывает метрики ККД по накопленным счётчикам.
     * Доли выражаются в процентах, а среднее количество проблем на цель — числом.
     * Результаты округляются до четырёх знаков после запятой. Если знаменатель
     * отдельной метрики равен нулю, её значение равно {@code null}.
     *
     * @return значения четырёх метрик ККД
     */
    public MetricValues values() {
        BigDecimal cleanGoalsShare = ratio(cleanGoals, activeGoals, 100);
        BigDecimal issuesPerGoal = ratio(issues, activeGoals, 1);
        BigDecimal goalsWithOwnerShare = ratio(goalsWithOwner, activeGoals, 100);
        BigDecimal validWeightsShare = ratio(goalsWithValidWeights, goalsWithKeyResults, 100);
        return new MetricValues(cleanGoalsShare, issuesPerGoal,
                goalsWithOwnerShare, validWeightsShare);
    }

    private static BigDecimal ratio(long numerator, long denominator, int multiplier) {
        if (denominator == 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(multiplier))
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
