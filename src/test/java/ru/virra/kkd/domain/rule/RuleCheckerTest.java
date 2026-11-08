package ru.virra.kkd.domain.rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.GoalStatus;
import ru.virra.kkd.domain.model.IssueObjectType;
import ru.virra.kkd.domain.model.IssueSeverity;
import ru.virra.kkd.domain.model.KeyResultData;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleCheckerTest {

    private static final LocalDate SNAPSHOT = LocalDate.of(2026, 10, 26);
    private final RuleChecker checker = new RuleChecker();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void reportsActiveGoalWithoutOwner(String ownerId) {
        GoalData goal = new GoalData(1, GoalStatus.ACTIVE, ownerId,
                List.of(new KeyResultData(10, 100, SNAPSHOT)));

        List<Finding> findings = checker.checkGoal(goal, SNAPSHOT);

        assertEquals(1, findings.size());
        Finding finding = findings.get(0);
        assertEquals("GOAL_NO_OWNER", finding.ruleCode());
        assertEquals(IssueObjectType.GOAL, finding.objectType());
        assertEquals(1, finding.objectId());
        assertEquals(1, finding.goalId());
        assertEquals(IssueSeverity.CRITICAL, finding.severity());
    }

    @Test
    void doesNotReportGoalWithOwner() {
        GoalData goal = new GoalData(1, GoalStatus.ACTIVE, "employee-1",
                List.of(new KeyResultData(10, 100, SNAPSHOT)));

        assertEquals(List.of(), checker.checkGoal(goal, SNAPSHOT));
    }

    @Test
    void reportsActiveGoalWithoutKeyResults() {
        GoalData goal = new GoalData(1, GoalStatus.ACTIVE, "employee-1", List.of());

        List<Finding> findings = checker.checkGoal(goal, SNAPSHOT);

        assertEquals(1, findings.size());
        Finding finding = findings.get(0);
        assertEquals("GOAL_NO_KR", finding.ruleCode());
        assertEquals(IssueObjectType.GOAL, finding.objectType());
        assertEquals(1, finding.objectId());
        assertEquals(1, finding.goalId());
        assertEquals(IssueSeverity.CRITICAL, finding.severity());
    }

    @ParameterizedTest
    @EnumSource(value = GoalStatus.class, names = {"DRAFT", "COMPLETED", "CANCELLED"})
    void ignoresInactiveGoals(GoalStatus status) {
        GoalData goal = new GoalData(1, status, null, List.of());

        assertEquals(List.of(), checker.checkGoal(goal, SNAPSHOT));
    }

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
