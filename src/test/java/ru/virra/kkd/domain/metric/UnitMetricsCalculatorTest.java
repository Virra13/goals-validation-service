package ru.virra.kkd.domain.metric;

import org.junit.jupiter.api.Test;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.GoalStatus;
import ru.virra.kkd.domain.model.KeyResultData;
import ru.virra.kkd.domain.rule.RuleChecker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnitMetricsCalculatorTest {
    private final UnitMetricsCalculator calculator = new UnitMetricsCalculator();

    @Test
    void sumsCountsAcrossLevelsRatherThanAveragingPercentages() {
        Map<Long, Long> parents = new HashMap<>();
        parents.put(1L, null);
        parents.put(2L, 1L);
        parents.put(3L, 1L);
        parents.put(4L, 2L);
        LocalDate date = LocalDate.of(2026, 10, 26);
        var kr = List.of(new KeyResultData(10, 100, date));
        var goals = List.of(new GoalData(1, GoalStatus.ACTIVE, "owner", kr),
                new GoalData(2, GoalStatus.ACTIVE, null, List.of()),
                new GoalData(3, GoalStatus.ACTIVE, "owner", kr));
        var checker = new RuleChecker();
        var findings = goals.stream().flatMap(g -> checker.checkGoal(g, date).stream()).toList();
        var result = calculator.calculate(parents, goals, findings, Map.of(1L, 1L, 2L, 2L, 3L, 4L));

        assertEquals(new BigDecimal("66.6667"), result.get(1L).cleanGoalsShare());
        assertEquals(new BigDecimal("0.6667"), result.get(1L).issuesPerGoal());
        assertEquals(new BigDecimal("66.6667"), result.get(1L).goalsWithOwnerShare());
        assertEquals(new BigDecimal("100.0000"), result.get(1L).goalsWithValidKrWeightsShare());
        assertEquals(new BigDecimal("50.0000"), result.get(2L).cleanGoalsShare());
        assertEquals(new BigDecimal("100.0000"), result.get(4L).cleanGoalsShare());
        assertEquals(new MetricValues(null, null, null, null), result.get(3L));
    }

    @Test
    void handlesDeepHierarchyWithoutRecursion() {
        Map<Long, Long> parents = new HashMap<>();
        parents.put(1L, null);
        for (long id = 2; id <= 12000; id++) parents.put(id, id - 1);
        assertEquals(12000, calculator.calculate(parents, List.of(), List.of(), Map.of()).size());
    }

    @Test
    void rejectsCyclesAndUnknownParents() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(Map.of(1L, 2L, 2L, 1L), List.of(), List.of(), Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(Map.of(1L, 99L), List.of(), List.of(), Map.of()));
    }
}
