package ru.virra.kkd.domain.metric;

import java.math.BigDecimal;
import java.util.function.Function;

public enum MetricKey {
    CLEAN_GOALS_SHARE("clean_goals_share", true, MetricValues::cleanGoalsShare),
    ISSUES_PER_GOAL("issues_per_goal", false, MetricValues::issuesPerGoal),
    GOALS_WITH_OWNER_SHARE("goals_with_owner_share", true, MetricValues::goalsWithOwnerShare),
    GOALS_WITH_VALID_KR_WEIGHTS_SHARE("goals_with_valid_kr_weights_share", true,
            MetricValues::goalsWithValidKrWeightsShare);

    private final String code;
    private final boolean higherIsBetter;
    private final Function<MetricValues, BigDecimal> readValue;

    MetricKey(String code, boolean higherIsBetter,
              Function<MetricValues, BigDecimal> readValue) {
        this.code = code;
        this.higherIsBetter = higherIsBetter;
        this.readValue = readValue;
    }

    public String code() {
        return code;
    }

    public boolean higherIsBetter() {
        return higherIsBetter;
    }

    public BigDecimal valueOf(MetricValues values) {
        return readValue.apply(values);
    }
}
