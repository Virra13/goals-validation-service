package ru.virra.kkd.domain.rule;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.GoalStatus;
import ru.virra.kkd.domain.model.KeyResultData;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleCheckerTest {

    private static final LocalDate SNAPSHOT = LocalDate.of(2026, 10, 26);
    private final RuleChecker checker = new RuleChecker();

    @ParameterizedTest
    @CsvSource({"49, true", "50, false", "51, true", ", true"})
    void checksWeightSum(Integer secondWeight, boolean expectedViolation) {
        GoalData goal = new GoalData(1, GoalStatus.ACTIVE, "employee-1", List.of(
                new KeyResultData(10, 50, SNAPSHOT),
                new KeyResultData(11, secondWeight, SNAPSHOT)
        ));

        assertEquals(expectedViolation ? List.of("KR_WEIGHTS_SUM") : List.of(),
                ruleCodes(goal));
    }

    @ParameterizedTest
    @CsvSource({"44, false", "45, false", "46, true", ", true"})
    void checksLastFactUpdate(Integer daysAgo, boolean expectedViolation) {
        LocalDate updated = daysAgo == null ? null : SNAPSHOT.minusDays(daysAgo);
        GoalData goal = new GoalData(1, GoalStatus.ACTIVE, "employee-1",
                List.of(new KeyResultData(10, 100, updated)));

        assertEquals(expectedViolation ? List.of("KR_STALE_PROGRESS") : List.of(),
                ruleCodes(goal));
    }

    private List<String> ruleCodes(GoalData goal) {
        return checker.checkGoal(goal, SNAPSHOT).stream()
                .map(Finding::ruleCode)
                .toList();
    }
}
