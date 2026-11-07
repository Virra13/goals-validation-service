package ru.virra.kkd.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.domain.metric.MetricValues;
import ru.virra.kkd.domain.metric.UnitMetricsCalculator;
import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;
import ru.virra.kkd.domain.model.GoalStatus;
import ru.virra.kkd.domain.model.KeyResultData;
import ru.virra.kkd.domain.model.RunStatus;
import ru.virra.kkd.domain.rule.RuleChecker;
import ru.virra.kkd.entity.Goal;
import ru.virra.kkd.entity.KeyResult;
import ru.virra.kkd.entity.KkdRule;
import ru.virra.kkd.entity.KkdRun;
import ru.virra.kkd.exception.InvalidKkdParameterException;
import ru.virra.kkd.exception.KkdResourceNotFoundException;
import ru.virra.kkd.repository.GoalRepository;
import ru.virra.kkd.repository.KeyResultRepository;
import ru.virra.kkd.repository.KkdResultWriter;
import ru.virra.kkd.repository.KkdRuleRepository;
import ru.virra.kkd.repository.KkdRunRepository;
import ru.virra.kkd.repository.UnitRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class KkdRunExecutionService {
    private final GoalRepository goals;
    private final KeyResultRepository keyResults;
    private final UnitRepository units;
    private final KkdRuleRepository rules;
    private final KkdRunRepository runs;
    private final KkdResultWriter results;
    private final Clock clock;
    private final RuleChecker ruleChecker = new RuleChecker();
    private final UnitMetricsCalculator metricsCalculator = new UnitMetricsCalculator();

    public KkdRunExecutionService(GoalRepository goals, KeyResultRepository keyResults,
                                  UnitRepository units, KkdRuleRepository rules,
                                  KkdRunRepository runs, KkdResultWriter results, Optional<Clock> clock) {
        this.goals = goals;
        this.keyResults = keyResults;
        this.units = units;
        this.rules = rules;
        this.runs = runs;
        this.results = results;
        this.clock = clock.orElseGet(Clock::systemUTC);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public long execute(Long comparedToRunId) {
        Instant startedAt = clock.instant();
        LocalDate snapshotDate = startedAt.atOffset(ZoneOffset.UTC).toLocalDate();
        KkdRun comparedTo = resolvePreviousRun(comparedToRunId);
        List<Goal> activeGoals = goals.findAllByStatus(GoalStatus.ACTIVE);
        List<GoalData> goalData = loadGoalData(activeGoals);
        Map<Long, Long> unitByGoal = activeGoals.stream()
                .collect(Collectors.toMap(Goal::getId, goal -> goal.getUnit().getId()));
        List<Finding> findings = goalData.stream()
                .flatMap(goal -> ruleChecker.checkGoal(goal, snapshotDate).stream())
                .toList();

        // Root parent IDs are null: Collectors.toMap does not support null values.
        Map<Long, Long> parentByUnit = new HashMap<>();
        units.findHierarchy().forEach(unit -> parentByUnit.put(unit.id(), unit.parentId()));
        Map<Long, MetricValues> values = metricsCalculator.calculate(parentByUnit, goalData,
                findings, unitByGoal);
        Map<String, Long> ruleIds = rules.findAll().stream()
                .collect(Collectors.toMap(KkdRule::getCode, KkdRule::getId));

        KkdRun run = new KkdRun();
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(startedAt);
        run.setSnapshotDate(startedAt);
        run.setComparedToRun(comparedTo);
        // JDBC writes use the same DataSource/transaction; flush the parent before its FK children.
        runs.saveAndFlush(run);
        results.saveIssues(run.getId(), findings, unitByGoal, ruleIds, startedAt);
        results.saveMetrics(run.getId(), values, startedAt);
        run.setCompletedAt(clock.instant());
        run.setStatus(RunStatus.COMPLETED);
        return run.getId();
    }

    private KkdRun resolvePreviousRun(Long comparedToRunId) {
        if (comparedToRunId == null) return null;
        if (comparedToRunId <= 0) {
            throw new InvalidKkdParameterException("ID прогона должен быть больше нуля");
        }
        KkdRun previous = runs.findById(comparedToRunId)
                .orElseThrow(() -> new KkdResourceNotFoundException(
                        "Прогон " + comparedToRunId + " не найден"));
        if (previous.getStatus() != RunStatus.COMPLETED) {
            throw new InvalidKkdParameterException("Сравнение возможно только с завершённым прогоном");
        }
        return previous;
    }

    private List<GoalData> loadGoalData(List<Goal> activeGoals) {
        if (activeGoals.isEmpty()) return List.of();
        Map<Long, List<KeyResultData>> keyResultsByGoal = new HashMap<>();
        for (KeyResult keyResult : keyResults.findAllByGoalStatus(GoalStatus.ACTIVE)) {
            Instant lastUpdate = keyResult.getLastFactUpdate();
            LocalDate updateDate = lastUpdate == null ? null
                    : lastUpdate.atOffset(ZoneOffset.UTC).toLocalDate();
            KeyResultData data = new KeyResultData(keyResult.getId(), keyResult.getWeight(), updateDate);
            keyResultsByGoal.computeIfAbsent(keyResult.getGoal().getId(), id -> new ArrayList<>()).add(data);
        }
        return activeGoals.stream().map(goal -> new GoalData(goal.getId(), goal.getStatus(),
                goal.getOwnerId(), keyResultsByGoal.getOrDefault(goal.getId(), List.of()))).toList();
    }
}
