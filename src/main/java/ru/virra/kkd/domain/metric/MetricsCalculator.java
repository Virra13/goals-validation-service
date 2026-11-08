package ru.virra.kkd.domain.metric;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.GoalStatus;
import ru.virra.kkd.domain.model.GoalQuality;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Рассчитывает метрики ККД для переданного набора целей и найденных проблем.
 * Учитывает только активные цели. Самостоятельно не обходит иерархию подразделений;
 * суммирование счётчиков по поддереву выполняет {@link UnitMetricsCalculator}.
 */
public final class MetricsCalculator {

    /**
     * Рассчитывает значения метрик по активным целям и связанным с ними проблемам.
     *
     * @param goals цели для расчёта
     * @param findings проблемы, связанные с целями по идентификатору цели
     * @return рассчитанные значения; метрика с нулевым знаменателем равна {@code null}
     */
    public MetricValues calculate(Collection<GoalData> goals,
                                  Collection<Finding> findings) {
        return count(goals, findings).values();
    }

    /**
     * Подсчитывает исходные величины для метрик без вычисления долей.
     * Результат можно суммировать с показателями дочерних подразделений.
     *
     * @param goals цели для расчёта; неактивные цели пропускаются
     * @param findings проблемы, связанные с целями по идентификатору цели
     * @return счётчики активных целей и найденных для них проблем
     */
    public MetricCounts count(Collection<GoalData> goals, Collection<Finding> findings) {
        Map<Long, List<Finding>> findingsByGoal = findings.stream()
                .collect(Collectors.groupingBy(Finding::goalId));

        long activeGoals = 0;
        long cleanGoals = 0;
        long issueCount = 0;
        long goalsWithOwner = 0;
        long goalsWithKeyResults = 0;
        long goalsWithValidWeights = 0;

        for (GoalData goal : goals) {
            if (goal.status() != GoalStatus.ACTIVE) {
                continue;
            }

            activeGoals++;
            List<Finding> goalFindings = findingsByGoal.getOrDefault(goal.id(), List.of());
            issueCount += goalFindings.size();

            if (goalFindings.isEmpty()) {
                cleanGoals++;
            }
            if (GoalQuality.hasOwner(goal)) {
                goalsWithOwner++;
            }
            if (!goal.keyResults().isEmpty()) {
                goalsWithKeyResults++;
                if (GoalQuality.hasValidWeights(goal)) {
                    goalsWithValidWeights++;
                }
            }
        }

        return new MetricCounts(activeGoals, cleanGoals, issueCount, goalsWithOwner,
                goalsWithKeyResults, goalsWithValidWeights);
    }
}
