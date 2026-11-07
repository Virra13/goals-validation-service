package ru.virra.kkd.domain.metric;

import org.junit.jupiter.api.Test;
import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.GoalStatus;
import ru.virra.kkd.domain.model.KeyResultData;
import ru.virra.kkd.domain.rule.RuleChecker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MetricsCalculatorTest {

    private static final LocalDate SNAPSHOT = LocalDate.of(2026, 10, 26);

    private final RuleChecker checker = new RuleChecker();
    private final MetricsCalculator calculator = new MetricsCalculator();

    @Test
    void ownerAndWeightMetricsDoNotDependOnRuleFindings() {
        GoalData goal = new GoalData(1, GoalStatus.ACTIVE, " ",
                List.of(new KeyResultData(10, 90, SNAPSHOT)));
        MetricValues values = calculator.calculate(List.of(goal), List.of());
        assertEquals(new BigDecimal("0.0000"), values.goalsWithOwnerShare());
        assertEquals(new BigDecimal("0.0000"), values.goalsWithValidKrWeightsShare());
    }

    @Test
    void returnsNullForAllMetricsWithoutActiveGoals() {
        assertEquals(new MetricValues(null, null, null, null), calculator.calculate(List.of(), List.of()));
    }

    @Test
    void calculatesMetricsForActiveGoals() {
        List<GoalData> goals = List.of(
                new GoalData(1, GoalStatus.ACTIVE, "employee-1",
                        List.of(new KeyResultData(101, 100, SNAPSHOT))),
                new GoalData(2, GoalStatus.ACTIVE, " ",
                        List.of(new KeyResultData(102, 100, SNAPSHOT.minusDays(46)))),
                new GoalData(3, GoalStatus.ACTIVE, "employee-3", List.of()),
                new GoalData(4, GoalStatus.ACTIVE, "employee-4",
                        List.of(new KeyResultData(104, 90, SNAPSHOT))),
                new GoalData(5, GoalStatus.DRAFT, null, List.of())
        );
        List<Finding> findings = goals.stream()
                .flatMap(goal -> checker.checkGoal(goal, SNAPSHOT).stream())
                .toList();

        MetricValues result = calculator.calculate(goals, findings);

        assertEquals(new BigDecimal("25.0000"), result.cleanGoalsShare());
        assertEquals(new BigDecimal("1.0000"), result.issuesPerGoal());
        assertEquals(new BigDecimal("75.0000"), result.goalsWithOwnerShare());
        assertEquals(new BigDecimal("66.6667"), result.goalsWithValidKrWeightsShare());
    }

    @Test
    void returnsNullForWeightMetricWhenNoActiveGoalHasKeyResults() {
        GoalData goal = new GoalData(1, GoalStatus.ACTIVE, "employee-1", List.of());
        MetricValues result = calculator.calculate(List.of(goal), checker.checkGoal(goal, SNAPSHOT));

        assertNull(result.goalsWithValidKrWeightsShare());
        assertEquals(new BigDecimal("0.0000"), result.cleanGoalsShare());
    }
}
