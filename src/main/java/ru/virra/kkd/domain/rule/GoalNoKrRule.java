package ru.virra.kkd.domain.rule;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.Severity;

import java.time.LocalDate;
import java.util.List;

final class GoalNoKrRule implements QualityRule {

    @Override
    public List<Finding> check(GoalData goal, LocalDate snapshotDate) {
        if (!goal.keyResults().isEmpty()) {
            return List.of();
        }
        return List.of(Finding.forGoal("GOAL_NO_KR", goal.id(), Severity.CRITICAL,
                "У активной цели нет ключевых результатов."));
    }
}
