package ru.virra.kkd.readmodel;

import ru.virra.kkd.domain.comparison.IssueDelta;
import ru.virra.kkd.domain.comparison.MetricChange;
import ru.virra.kkd.domain.metric.MetricKey;

import java.util.List;
import java.util.Map;

public record KkdSummary(
        RunReference run,
        RunReference comparedTo,
        UnitReference unit,
        Map<MetricKey, MetricChange> metrics,
        List<SeverityChange> issuesBySeverity,
        IssueDelta delta
) {
}
