package ru.virra.kkd.domain.metric;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Рассчитывает метрики ККД для каждого подразделения с учётом его поддерева.
 * Суммирует счётчики от дочерних подразделений к родительским без рекурсии,
 * затем вычисляет доли для каждого накопленного набора счётчиков.
 */
public final class UnitMetricsCalculator {
    private final MetricsCalculator calculator = new MetricsCalculator();

    /**
     * Возвращает метрики для всех подразделений, включая подразделения без активных целей.
     * Сначала учитываются собственные цели подразделения, затем счётчики всех потомков.
     * Циклы и ссылки на неизвестные подразделения отклоняются.
     *
     * @param parentByUnit соответствие идентификатора подразделения и его родителя;
     *                     у подразделения верхнего уровня родитель равен {@code null}
     * @param goals цели для расчёта
     * @param findings проблемы, связанные с целями по идентификатору цели
     * @param unitByGoal соответствие идентификаторов целей и подразделений
     * @return значения метрик для каждого подразделения с учётом его поддерева
     * @throws IllegalArgumentException если иерархия содержит цикл, неизвестного родителя
     *                                  или данные относятся к неизвестному подразделению
     */
    public Map<Long, MetricValues> calculate(Map<Long, Long> parentByUnit,
                                            Collection<GoalData> goals, Collection<Finding> findings,
                                            Map<Long, Long> unitByGoal) {
        Map<Long, List<GoalData>> goalsByUnit = groupGoalsByUnit(goals, unitByGoal);
        Map<Long, List<Finding>> findingsByUnit = groupFindingsByUnit(findings, unitByGoal);
        validateUnits(parentByUnit, goalsByUnit, findingsByUnit);

        Map<Long, MetricCounts> counts = countOwnData(parentByUnit, goalsByUnit, findingsByUnit);
        Map<Long, Integer> remainingChildren = countChildren(parentByUnit);
        return sumFromLeaves(parentByUnit, counts, remainingChildren);
    }

    private static Map<Long, List<GoalData>> groupGoalsByUnit(
            Collection<GoalData> goals, Map<Long, Long> unitByGoal) {
        Map<Long, List<GoalData>> grouped = new HashMap<>();
        for (GoalData goal : goals) {
            Long unitId = unitByGoal.get(goal.id());
            grouped.computeIfAbsent(unitId, id -> new ArrayList<>()).add(goal);
        }
        return grouped;
    }

    private static Map<Long, List<Finding>> groupFindingsByUnit(
            Collection<Finding> findings, Map<Long, Long> unitByGoal) {
        Map<Long, List<Finding>> grouped = new HashMap<>();
        for (Finding finding : findings) {
            Long unitId = unitByGoal.get(finding.goalId());
            grouped.computeIfAbsent(unitId, id -> new ArrayList<>()).add(finding);
        }
        return grouped;
    }

    private static void validateUnits(Map<Long, Long> parentByUnit,
                                      Map<Long, List<GoalData>> goalsByUnit,
                                      Map<Long, List<Finding>> findingsByUnit) {
        if (!parentByUnit.keySet().containsAll(goalsByUnit.keySet())
                || !parentByUnit.keySet().containsAll(findingsByUnit.keySet())) {
            throw new IllegalArgumentException("Данные относятся к неизвестному подразделению");
        }
    }

    private Map<Long, MetricCounts> countOwnData(Map<Long, Long> parentByUnit,
                                                 Map<Long, List<GoalData>> goalsByUnit,
                                                 Map<Long, List<Finding>> findingsByUnit) {
        Map<Long, MetricCounts> counts = new HashMap<>();
        for (Long unitId : parentByUnit.keySet()) {
            List<GoalData> unitGoals = goalsByUnit.getOrDefault(unitId, List.of());
            List<Finding> unitFindings = findingsByUnit.getOrDefault(unitId, List.of());
            counts.put(unitId, calculator.count(unitGoals, unitFindings));
        }
        return counts;
    }

    private static Map<Long, Integer> countChildren(Map<Long, Long> parentByUnit) {
        Map<Long, Integer> remainingChildren = new HashMap<>();
        for (Long unitId : parentByUnit.keySet()) {
            remainingChildren.put(unitId, 0);
        }
        for (Long parentId : parentByUnit.values()) {
            if (parentId == null) {
                continue;
            }
            if (!parentByUnit.containsKey(parentId)) {
                throw new IllegalArgumentException("Неизвестное родительское подразделение " + parentId);
            }
            remainingChildren.put(parentId, remainingChildren.get(parentId) + 1);
        }
        return remainingChildren;
    }

    private static Map<Long, MetricValues> sumFromLeaves(Map<Long, Long> parentByUnit,
                                                          Map<Long, MetricCounts> counts,
                                                          Map<Long, Integer> remainingChildren) {
        ArrayDeque<Long> leaves = new ArrayDeque<>();
        for (Map.Entry<Long, Integer> entry : remainingChildren.entrySet()) {
            if (entry.getValue() == 0) {
                leaves.add(entry.getKey());
            }
        }
        Map<Long, MetricValues> values = new HashMap<>();
        while (!leaves.isEmpty()) {
            long unitId = leaves.remove();
            MetricCounts total = counts.get(unitId);
            // Calculate ratios only after all child counts have reached this unit.
            values.put(unitId, total.values());
            Long parentId = parentByUnit.get(unitId);
            if (parentId != null) {
                counts.put(parentId, counts.get(parentId).plus(total));
                int childrenLeft = remainingChildren.get(parentId) - 1;
                remainingChildren.put(parentId, childrenLeft);
                if (childrenLeft == 0) {
                    leaves.add(parentId);
                }
            }
        }
        if (values.size() != parentByUnit.size()) {
            throw new IllegalArgumentException("Цикл в иерархии подразделений");
        }
        return Map.copyOf(values);
    }
}
