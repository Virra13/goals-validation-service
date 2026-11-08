package ru.virra.kkd.domain.rule;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;

import java.time.LocalDate;
import java.util.List;

/**
 * Контракт отдельного правила проверки качества данных активной цели.
 * Правила получают подготовленные данные цели и не обращаются к REST API или БД.
 */
interface QualityRule {
    /**
     * Проверяет цель на дату снимка прогона.
     *
     * @param goal данные активной цели и её ключевых результатов
     * @param snapshotDate календарная дата снимка прогона в UTC
     * @return найденные проблемы или пустой список, если правило не нарушено
     */
    List<Finding> check(GoalData goal, LocalDate snapshotDate);
}
