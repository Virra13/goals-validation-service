package ru.virra.kkd.domain.comparison;

import org.junit.jupiter.api.Test;
import ru.virra.kkd.domain.metric.MetricKey;
import ru.virra.kkd.domain.metric.MetricValues;
import ru.virra.kkd.domain.model.IssueObjectType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RunComparatorTest {

    private final RunComparator comparator = new RunComparator();

    @Test
    void comparesMetricValuesUsingTheDirectionOfEachMetric() {
        MetricValues current = new MetricValues(new BigDecimal("65"), new BigDecimal("0.50"),
                new BigDecimal("100"), null);
        MetricValues previous = new MetricValues(new BigDecimal("70"), new BigDecimal("0.75"),
                new BigDecimal("100"), new BigDecimal("80"));

        Map<MetricKey, MetricChange> changes = comparator.compareMetrics(current, previous);

        assertEquals(new MetricChange(new BigDecimal("65"), new BigDecimal("70"),
                new BigDecimal("-5"), Trend.WORSE), changes.get(MetricKey.CLEAN_GOALS_SHARE));
        assertEquals(new MetricChange(new BigDecimal("0.50"), new BigDecimal("0.75"),
                new BigDecimal("-0.25"), Trend.BETTER), changes.get(MetricKey.ISSUES_PER_GOAL));
        assertEquals(new MetricChange(new BigDecimal("100"), new BigDecimal("100"),
                BigDecimal.ZERO, Trend.SAME), changes.get(MetricKey.GOALS_WITH_OWNER_SHARE));
        assertEquals(new MetricChange(null, new BigDecimal("80"), null, null),
                changes.get(MetricKey.GOALS_WITH_VALID_KR_WEIGHTS_SHARE));
    }

    @Test
    void comparesIssueKeysIncludingTheObjectType() {
        IssueKey persisting = new IssueKey(1, IssueObjectType.GOAL, 42);
        IssueKey resolved = new IssueKey(2, IssueObjectType.GOAL, 105);
        IssueKey introduced = new IssueKey(2, IssueObjectType.KEY_RESULT, 105);

        assertEquals(new IssueDelta(1, 1, 1),
                comparator.compareIssues(Set.of(persisting, introduced), Set.of(persisting, resolved)));
    }

    @Test
    void distinguishesMissingPreviousRunFromPreviousRunWithoutIssues() {
        MetricValues current = new MetricValues(new BigDecimal("65"), new BigDecimal("0.50"),
                new BigDecimal("100"), new BigDecimal("80"));
        Set<IssueKey> issues = Set.of(new IssueKey(1, IssueObjectType.GOAL, 42));

        Map<MetricKey, MetricChange> changes = comparator.compareMetrics(current, null);
        for (MetricKey key : MetricKey.values()) {
            assertEquals(new MetricChange(key.valueOf(current), null, null, null), changes.get(key));
        }

        assertNull(comparator.compareIssues(issues, null));
        assertEquals(new IssueDelta(1, 0, 0), comparator.compareIssues(issues, Set.of()));
    }
}
