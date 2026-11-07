package ru.virra.kkd.domain.metric;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Iterative bottom-up aggregation, O(units + goals + key results + findings), without a depth limit. */
public final class UnitMetricsCalculator {
    private final MetricsCalculator calculator = new MetricsCalculator();

    public Map<Long, MetricValues> calculate(Map<Long, Long> parentByUnit,
                                            Collection<GoalData> goals, Collection<Finding> findings,
                                            Map<Long, Long> unitByGoal) {
        Map<Long, List<GoalData>> goalsByUnit = goals.stream()
                .collect(Collectors.groupingBy(goal -> unitByGoal.get(goal.id())));
        Map<Long, List<Finding>> findingsByUnit = findings.stream()
                .collect(Collectors.groupingBy(finding -> unitByGoal.get(finding.goalId())));
        if (!parentByUnit.keySet().containsAll(goalsByUnit.keySet())
                || !parentByUnit.keySet().containsAll(findingsByUnit.keySet())) {
            throw new IllegalArgumentException("Данные относятся к неизвестному подразделению");
        }

        Map<Long, Integer> remainingChildren = new HashMap<>();
        Map<Long, MetricCounts> counts = new HashMap<>();
        for (long unitId : parentByUnit.keySet()) {
            remainingChildren.put(unitId, 0);
            counts.put(unitId, calculator.count(goalsByUnit.getOrDefault(unitId, List.of()),
                    findingsByUnit.getOrDefault(unitId, List.of())));
        }
        for (Long parentId : parentByUnit.values()) {
            if (parentId != null) {
                if (!parentByUnit.containsKey(parentId)) {
                    throw new IllegalArgumentException("Неизвестное родительское подразделение " + parentId);
                }
                remainingChildren.merge(parentId, 1, Integer::sum);
            }
        }

        ArrayDeque<Long> leaves = new ArrayDeque<>();
        remainingChildren.forEach((id, children) -> {
            if (children == 0) leaves.add(id);
        });
        Map<Long, MetricValues> values = new HashMap<>();
        while (!leaves.isEmpty()) {
            long unitId = leaves.remove();
            MetricCounts total = counts.get(unitId);
            values.put(unitId, total.values());
            Long parentId = parentByUnit.get(unitId);
            if (parentId != null) {
                counts.merge(parentId, total, MetricCounts::plus);
                if (remainingChildren.merge(parentId, -1, Integer::sum) == 0) {
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
