package ru.virra.kkd.domain.comparison;

import ru.virra.kkd.domain.metric.MetricKey;
import ru.virra.kkd.domain.metric.MetricValues;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class RunComparator {

    public Map<MetricKey, MetricChange> compareMetrics(
            MetricValues current, MetricValues previous) {
        var changes = new EnumMap<MetricKey, MetricChange>(MetricKey.class);

        for (MetricKey key : MetricKey.values()) {
            BigDecimal value = key.valueOf(current);
            BigDecimal previousValue = previous == null ? null : key.valueOf(previous);
            changes.put(key, compareMetric(key, value, previousValue));
        }

        return Collections.unmodifiableMap(changes);
    }

    public IssueDelta compareIssues(Set<IssueKey> current, Set<IssueKey> previous) {
        if (previous == null) {
            return null;
        }

        long persisting = current.stream().filter(previous::contains).count();
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

        boolean improved = (sign > 0) == key.higherIsBetter();
        return new MetricChange(value, previous, delta,
                improved ? Trend.BETTER : Trend.WORSE);
    }
}
