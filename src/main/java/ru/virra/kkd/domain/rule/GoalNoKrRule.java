package ru.virra.kkd.domain.rule;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.IssueSeverity;

import java.time.LocalDate;
import java.util.List;

/**
 * Реализует правило ККД {@code GOAL_NO_KR}.
 * Создаёт проблему важности {@code CRITICAL}, если у активной цели
 * нет ключевых результатов.
 */
final class GoalNoKrRule implements QualityRule {

    @Override
    public List<Finding> check(GoalData goal, LocalDate snapshotDate) {
        if (!goal.keyResults().isEmpty()) {
            return List.of();
        }
        return List.of(Finding.forGoal("GOAL_NO_KR", goal.id(), IssueSeverity.CRITICAL,
                "У активной цели нет ключевых результатов."));
    }
}
