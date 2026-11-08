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

/**
 * Координирует выполнение прогона ККД.
 * Получает активные цели и связанные с ними ключевые результаты, применяет правила
 * проверки, рассчитывает метрики по подразделениям и сохраняет результаты прогона.
 * Проверка, расчёт и запись выполняются в одной транзакции.
 */
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

    /**
     * Создаёт сервис выполнения прогона.
     * Если бин {@link Clock} не задан, используется системное время UTC.
     *
     * @param goals репозиторий целей
     * @param keyResults репозиторий ключевых результатов
     * @param units репозиторий подразделений
     * @param rules репозиторий справочника правил ККД
     * @param runs репозиторий прогонов
     * @param results компонент сохранения проблем и метрик прогона
     * @param clock необязательные часы, позволяющие фиксировать время в тестах
     */
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

    /**
     * Выполняет проверку активных целей и сохраняет новый прогон ККД.
     * Данные читаются из одного снимка БД при уровне изоляции {@code REPEATABLE_READ}.
     * Дата снимка определяется по времени начала прогона в UTC.
     * При ошибке сохранение прогона, проблем и метрик откатывается.
     *
     * @param comparedToRunId идентификатор завершённого прогона для сравнения
     *                        или {@code null}, если сравнение не требуется
     * @return идентификатор сохранённого прогона со статусом {@code COMPLETED}
     * @throws InvalidKkdParameterException если идентификатор не положителен
     *                                      или прогон для сравнения не завершён
     * @throws KkdResourceNotFoundException если прогон для сравнения не найден
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public long execute(Long comparedToRunId) {
        Instant startedAt = clock.instant();
        LocalDate snapshotDate = startedAt.atOffset(ZoneOffset.UTC).toLocalDate();
        KkdRun comparedTo = resolvePreviousRun(comparedToRunId);

        List<Goal> activeGoals = goals.findAllByStatus(GoalStatus.ACTIVE);
        List<GoalData> goalData = loadGoalData(activeGoals);
        Map<Long, Long> unitByGoal = mapGoalsToUnits(activeGoals);
        List<Finding> findings = checkGoals(goalData, snapshotDate);

        Map<Long, Long> parentByUnit = loadUnitHierarchy();
        Map<Long, MetricValues> values = metricsCalculator.calculate(
                parentByUnit, goalData, findings, unitByGoal);
        Map<String, Long> ruleIds = loadRuleIds();

        KkdRun run = newRun(startedAt, comparedTo);
        runs.save(run);
        results.saveIssues(run.getId(), findings, unitByGoal, ruleIds, startedAt);
        results.saveMetrics(run.getId(), values, startedAt);
        run.setCompletedAt(clock.instant());
        run.setStatus(RunStatus.COMPLETED);
        return run.getId();
    }

    private Map<Long, Long> mapGoalsToUnits(List<Goal> activeGoals) {
        Map<Long, Long> unitByGoal = new HashMap<>();
        for (Goal goal : activeGoals) {
            unitByGoal.put(goal.getId(), goal.getUnit().getId());
        }
        return unitByGoal;
    }

    private List<Finding> checkGoals(List<GoalData> goalData, LocalDate snapshotDate) {
        List<Finding> findings = new ArrayList<>();
        for (GoalData goal : goalData) {
            findings.addAll(ruleChecker.checkGoal(goal, snapshotDate));
        }
        return findings;
    }

    private Map<Long, Long> loadUnitHierarchy() {
        // Root units have no parent, so their values are null.
        Map<Long, Long> parentByUnit = new HashMap<>();
        units.findHierarchy().forEach(unit -> parentByUnit.put(unit.id(), unit.parentId()));
        return parentByUnit;
    }

    private Map<String, Long> loadRuleIds() {
        Map<String, Long> ruleIds = new HashMap<>();
        for (KkdRule rule : rules.findAll()) {
            ruleIds.put(rule.getCode(), rule.getId());
        }
        return ruleIds;
    }

    private static KkdRun newRun(Instant startedAt, KkdRun comparedTo) {
        KkdRun run = new KkdRun();
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(startedAt);
        run.setSnapshotDate(startedAt);
        run.setComparedToRun(comparedTo);
        return run;
    }

    private KkdRun resolvePreviousRun(Long comparedToRunId) {
        if (comparedToRunId == null) {
            return null;
        }
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
        if (activeGoals.isEmpty()) {
            return List.of();
        }
        Map<Long, List<KeyResultData>> keyResultsByGoal = new HashMap<>();
        for (KeyResult keyResult : keyResults.findAllByGoalStatus(GoalStatus.ACTIVE)) {
            Instant lastUpdate = keyResult.getLastFactUpdate();
            LocalDate updateDate = lastUpdate == null
                    ? null : lastUpdate.atOffset(ZoneOffset.UTC).toLocalDate();
            KeyResultData data = new KeyResultData(keyResult.getId(), keyResult.getWeight(), updateDate);
            keyResultsByGoal.computeIfAbsent(keyResult.getGoal().getId(), id -> new ArrayList<>()).add(data);
        }
        List<GoalData> result = new ArrayList<>(activeGoals.size());
        for (Goal goal : activeGoals) {
            List<KeyResultData> goalKeyResults = keyResultsByGoal.getOrDefault(goal.getId(), List.of());
            result.add(new GoalData(goal.getId(), goal.getStatus(), goal.getOwnerId(), goalKeyResults));
        }
        return result;
    }
}
