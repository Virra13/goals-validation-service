package ru.virra.kkd.domain.rule;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.GoalStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class RuleChecker {

    private static final List<QualityRule> RULES = List.of(
            new GoalNoOwnerRule(),
            new GoalNoKrRule(),
            new KrWeightsSumRule(),
            new KrStaleProgressRule()
    );

    public List<Finding> checkGoal(GoalData goal, LocalDate snapshotDate) {
        if (goal.status() != GoalStatus.ACTIVE) {
            return List.of();
        }

        List<Finding> findings = new ArrayList<>();
        for (QualityRule rule : RULES) {
            findings.addAll(rule.check(goal, snapshotDate));
        }
        return List.copyOf(findings);
    }
}
