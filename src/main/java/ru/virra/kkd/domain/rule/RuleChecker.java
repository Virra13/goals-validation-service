package ru.virra.kkd.domain.rule;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.GoalStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Применяет правила ККД к данным цели и связанных с ней ключевых результатов.
 * Проверяются только активные цели; результаты отдельных правил объединяются
 * в список обнаруженных проблем без обращения к БД.
 */
public final class RuleChecker {

    private static final List<QualityRule> RULES = List.of(
            new GoalNoOwnerRule(),
            new GoalNoKrRule(),
            new KrWeightsSumRule(),
            new KrStaleProgressRule()
    );

    /**
     * Последовательно проверяет цель всеми правилами ККД.
     * Для неактивной цели правила не запускаются.
     *
     * @param goal данные цели и её ключевых результатов
     * @param snapshotDate календарная дата снимка прогона в UTC
     * @return неизменяемый список обнаруженных проблем или пустой список
     */
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
