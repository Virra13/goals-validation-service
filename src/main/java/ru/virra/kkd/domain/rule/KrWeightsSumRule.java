package ru.virra.kkd.domain.rule;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.GoalQuality;
import ru.virra.kkd.domain.model.KeyResultData;
import ru.virra.kkd.domain.model.Severity;

import java.time.LocalDate;
import java.util.List;

final class KrWeightsSumRule implements QualityRule {

    @Override
    public List<Finding> check(GoalData goal, LocalDate snapshotDate) {
        if (goal.keyResults().isEmpty() || GoalQuality.hasValidWeights(goal)) {
            return List.of();
        }

        long sum = 0;
        for (KeyResultData kr : goal.keyResults()) {
            if (kr.weight() == null) {
                return List.of(Finding.forGoal("KR_WEIGHTS_SUM", goal.id(), Severity.MAJOR,
                        "Не у всех ключевых результатов указан вес."));
            }
            sum += kr.weight();
        }

        return List.of(Finding.forGoal("KR_WEIGHTS_SUM", goal.id(), Severity.MAJOR,
                "Сумма весов ключевых результатов — " + sum + "%, ожидается 100%."));
    }
}
