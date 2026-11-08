package ru.virra.kkd.domain.metric;

import java.math.BigDecimal;

/**
 * Метрики ККД с кодами для сохранения и правилом определения улучшения.
 * Рост всех долей означает улучшение; рост среднего количества проблем на цель — ухудшение.
 */
public enum MetricKey {
    CLEAN_GOALS_SHARE("clean_goals_share", true),
    ISSUES_PER_GOAL("issues_per_goal", false),
    GOALS_WITH_OWNER_SHARE("goals_with_owner_share", true),
    GOALS_WITH_VALID_KR_WEIGHTS_SHARE("goals_with_valid_kr_weights_share", true);

    private final String code;
    private final boolean higherIsBetter;

    MetricKey(String code, boolean higherIsBetter) {
        this.code = code;
        this.higherIsBetter = higherIsBetter;
    }

    /**
     * Возвращает код метрики для сохранения и представления в HTTP API.
     *
     * @return строковый код метрики из контракта ККД
     */
    public String code() {
        return code;
    }

    /**
     * Определяет направление улучшения для сравнения прогонов.
     *
     * @return {@code true}, если рост значения метрики означает улучшение
     */
    public boolean higherIsBetter() {
        return higherIsBetter;
    }

    /**
     * Получает значение этой метрики из набора рассчитанных показателей.
     *
     * @param values рассчитанные показатели
     * @return значение метрики или {@code null} при нулевом знаменателе
     */
    public BigDecimal valueOf(MetricValues values) {
        return switch (this) {
            case CLEAN_GOALS_SHARE -> values.cleanGoalsShare();
            case ISSUES_PER_GOAL -> values.issuesPerGoal();
            case GOALS_WITH_OWNER_SHARE -> values.goalsWithOwnerShare();
            case GOALS_WITH_VALID_KR_WEIGHTS_SHARE -> values.goalsWithValidKrWeightsShare();
        };
    }
}
