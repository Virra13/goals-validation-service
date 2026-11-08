package ru.virra.kkd.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.domain.comparison.IssueDelta;
import ru.virra.kkd.domain.comparison.IssueKey;
import ru.virra.kkd.domain.comparison.MetricChange;
import ru.virra.kkd.domain.comparison.RunComparator;
import ru.virra.kkd.domain.metric.MetricKey;
import ru.virra.kkd.domain.metric.MetricValues;
import ru.virra.kkd.domain.model.IssueSeverity;
import ru.virra.kkd.repository.projection.IssueSummaryRow;
import ru.virra.kkd.entity.KkdRun;
import ru.virra.kkd.entity.KkdRunMetric;
import ru.virra.kkd.entity.Unit;
import ru.virra.kkd.exception.KkdResourceNotFoundException;
import ru.virra.kkd.readmodel.KkdSummary;
import ru.virra.kkd.readmodel.RunReference;
import ru.virra.kkd.readmodel.SeverityChange;
import ru.virra.kkd.readmodel.UnitReference;
import ru.virra.kkd.repository.KkdIssueRepository;
import ru.virra.kkd.repository.KkdRunMetricRepository;
import ru.virra.kkd.repository.KkdRunRepository;
import ru.virra.kkd.repository.UnitRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Формирует модель чтения сводной статистики ККД для прогона и подразделения.
 * Получает сохранённые метрики подразделения, проблемы его поддерева и результаты
 * сравнения с прогоном, указанным в {@code compared_to_run_id}.
 */
@Service
public class KkdSummaryService {

    private final KkdRunRepository runs;
    private final UnitRepository units;
    private final KkdRunMetricRepository metrics;
    private final KkdIssueRepository issues;
    private final RunComparator comparator = new RunComparator();

    public KkdSummaryService(KkdRunRepository runs, UnitRepository units,
                             KkdRunMetricRepository metrics, KkdIssueRepository issues) {
        this.runs = runs;
        this.units = units;
        this.metrics = metrics;
        this.issues = issues;
    }

    /**
     * Получает сводную статистику для подразделения с учётом его дочерних подразделений.
     * Метрики читаются из сохранённых значений для указанного подразделения;
     * проблемы отбираются по подразделениям текущего поддерева.
     * Если прогон для сравнения не указан, предыдущие значения и дельты отсутствуют.
     *
     * @param runId идентификатор текущего прогона ККД
     * @param unitId идентификатор выбранного подразделения
     * @return модель чтения с метриками, разбивкой проблем по важности и результатами сравнения
     * @throws KkdResourceNotFoundException если прогон или подразделение не найдены
     */
    @Transactional(readOnly = true)
    public KkdSummary getSummary(long runId, long unitId) {
        KkdRun run = findRun(runId);
        Unit unit = findUnit(unitId);
        KkdRun previousRun = run.getComparedToRun();
        List<Long> subtreeIds = units.findSubtreeIds(unitId);

        MetricValues currentMetrics = loadMetrics(runId, unitId);
        MetricValues previousMetrics = previousRun == null
                ? null : loadMetrics(previousRun.getId(), unitId);
        Map<MetricKey, MetricChange> metricChanges = comparator.compareMetrics(currentMetrics, previousMetrics);

        List<IssueSummaryRow> currentIssues = issues.findSummaryRows(runId, subtreeIds);
        List<IssueSummaryRow> previousIssues = previousRun == null
                ? null : issues.findSummaryRows(previousRun.getId(), subtreeIds);
        List<SeverityChange> issuesBySeverity = severityChanges(currentIssues, previousIssues);
        Set<IssueKey> currentKeys = issueKeys(currentIssues);
        Set<IssueKey> previousKeys = previousIssues == null ? null : issueKeys(previousIssues);
        IssueDelta issueDelta = comparator.compareIssues(currentKeys, previousKeys);

        RunReference runReference = reference(run);
        RunReference previousRunReference = previousRun == null ? null : reference(previousRun);
        UnitReference unitReference = new UnitReference(unit.getId(), unit.getName());

        return new KkdSummary(runReference, previousRunReference, unitReference,
                metricChanges, issuesBySeverity, issueDelta);
    }

    private KkdRun findRun(long runId) {
        return runs.findById(runId)
                .orElseThrow(() -> new KkdResourceNotFoundException(
                        "Прогон " + runId + " не найден"));
    }

    private Unit findUnit(long unitId) {
        return units.findById(unitId)
                .orElseThrow(() -> new KkdResourceNotFoundException(
                        "Подразделение " + unitId + " не найдено"));
    }

    private MetricValues loadMetrics(long runId, long unitId) {
        Map<String, BigDecimal> values = new HashMap<>();
        for (KkdRunMetric row : metrics.findAllByRunIdAndUnitId(runId, unitId)) {
            values.put(row.getMetricCode(), row.getValue());
        }
        return new MetricValues(
                values.get(MetricKey.CLEAN_GOALS_SHARE.code()),
                values.get(MetricKey.ISSUES_PER_GOAL.code()),
                values.get(MetricKey.GOALS_WITH_OWNER_SHARE.code()),
                values.get(MetricKey.GOALS_WITH_VALID_KR_WEIGHTS_SHARE.code())
        );
    }

    private static Set<IssueKey> issueKeys(List<IssueSummaryRow> issues) {
        Set<IssueKey> keys = new HashSet<>();
        for (IssueSummaryRow issue : issues) {
            keys.add(new IssueKey(issue.ruleId(), issue.objectType(), issue.objectId()));
        }
        return keys;
    }

    private static List<SeverityChange> severityChanges(List<IssueSummaryRow> currentIssues,
                                                        List<IssueSummaryRow> previousIssues) {
        Map<IssueSeverity, Long> currentCounts = countBySeverity(currentIssues);
        Map<IssueSeverity, Long> previousCounts = previousIssues == null ? null : countBySeverity(previousIssues);
        List<SeverityChange> changes = new ArrayList<>();
        for (IssueSeverity severity : IssueSeverity.values()) {
            long current = currentCounts.getOrDefault(severity, 0L);
            Long previous = previousCounts == null ? null : previousCounts.getOrDefault(severity, 0L);
            Long delta = previous == null ? null : current - previous;
            changes.add(new SeverityChange(severity, current, previous, delta));
        }
        return List.copyOf(changes);
    }

    private static Map<IssueSeverity, Long> countBySeverity(List<IssueSummaryRow> issues) {
        Map<IssueSeverity, Long> counts = new EnumMap<>(IssueSeverity.class);
        for (IssueSummaryRow issue : issues) {
            counts.merge(issue.severity(), 1L, Long::sum);
        }
        return counts;
    }

    private static RunReference reference(KkdRun run) {
        LocalDate snapshotDate = run.getSnapshotDate().atOffset(ZoneOffset.UTC).toLocalDate();
        return new RunReference(run.getId(), snapshotDate);
    }
}
