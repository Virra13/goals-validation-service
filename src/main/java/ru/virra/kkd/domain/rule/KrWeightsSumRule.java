package ru.virra.kkd.domain.rule;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.GoalQuality;
import ru.virra.kkd.domain.model.KeyResultData;
import ru.virra.kkd.domain.model.IssueSeverity;

import java.time.LocalDate;
import java.util.List;

/**
 * Реализует правило ККД {@code KR_WEIGHTS_SUM} для активных целей с КР.
 * Создаёт проблему важности {@code MAJOR}, если хотя бы один вес не задан
 * или сумма весов всех ключевых результатов отличается от 100.
 * Цель без КР проверяется правилом {@code GOAL_NO_KR}.
 */
final class KrWeightsSumRule implements QualityRule {

    @Override
    public List<Finding> check(GoalData goal, LocalDate snapshotDate) {
        if (goal.keyResults().isEmpty() || GoalQuality.hasValidWeights(goal)) {
            return List.of();
        }

        long sum = 0;
        for (KeyResultData kr : goal.keyResults()) {
            if (kr.weight() == null) {
                return List.of(Finding.forGoal("KR_WEIGHTS_SUM", goal.id(), IssueSeverity.MAJOR,
                        "Не у всех ключевых результатов указан вес."));
            }
            sum += kr.weight();
        }

        return List.of(Finding.forGoal("KR_WEIGHTS_SUM", goal.id(), IssueSeverity.MAJOR,
                "Сумма весов ключевых результатов — " + sum + "%, ожидается 100%."));
    }
}
