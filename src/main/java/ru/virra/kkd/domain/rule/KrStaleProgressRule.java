package ru.virra.kkd.domain.rule;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.IssueObjectType;
import ru.virra.kkd.domain.model.IssueSeverity;

import java.time.LocalDate;
import java.util.List;

/**
 * Реализует правило ККД {@code KR_STALE_PROGRESS} для КР активной цели.
 * Создаёт проблему важности {@code MINOR}, если дата обновления факта
 * отсутствует или предшествует дате снимка более чем на 45 календарных дней.
 * Ровно 45 дней нарушением не считается.
 */
final class KrStaleProgressRule implements QualityRule {

    @Override
    public List<Finding> check(GoalData goal, LocalDate snapshotDate) {
        LocalDate cutoff = snapshotDate.minusDays(45);

        return goal.keyResults().stream()
                .filter(kr -> kr.lastFactUpdate() == null
                        || kr.lastFactUpdate().isBefore(cutoff))
                .map(kr -> new Finding("KR_STALE_PROGRESS", IssueObjectType.KEY_RESULT,
                        kr.id(), goal.id(), IssueSeverity.MINOR,
                        kr.lastFactUpdate() == null
                                ? "Дата обновления факта не указана."
                                : "Факт не обновлялся более 45 дней."))
                .toList();
    }
}
