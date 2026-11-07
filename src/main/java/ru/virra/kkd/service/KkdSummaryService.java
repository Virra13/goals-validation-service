package ru.virra.kkd.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.domain.comparison.IssueDelta;
import ru.virra.kkd.domain.comparison.IssueKey;
import ru.virra.kkd.domain.comparison.MetricChange;
import ru.virra.kkd.domain.comparison.RunComparator;
import ru.virra.kkd.domain.metric.MetricKey;
import ru.virra.kkd.domain.metric.MetricValues;
import ru.virra.kkd.domain.model.Severity;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public KkdSummary getSummary(long runId, long unitId) {
        KkdRun run = runs.findById(runId)
                .orElseThrow(() -> new KkdResourceNotFoundException(
                        "Прогон " + runId + " не найден"));
        Unit unit = units.findById(unitId)
                .orElseThrow(() -> new KkdResourceNotFoundException(
                        "Подразделение " + unitId + " не найдено"));

        KkdRun previousRun = run.getComparedToRun();
        Long previousRunId = previousRun == null ? null : previousRun.getId();
        List<Long> subtreeIds = units.findSubtreeIds(unitId);

        MetricValues currentMetrics = loadMetrics(runId, unitId);
        MetricValues previousMetrics = previousRunId == null ? null : loadMetrics(previousRunId, unitId);
        Map<MetricKey, MetricChange> metricChanges = comparator.compareMetrics(currentMetrics, previousMetrics);

        List<IssueSummaryRow> currentIssues = issues.findSummaryRows(runId, subtreeIds);
        List<IssueSummaryRow> previousIssues = previousRunId == null
                ? null : issues.findSummaryRows(previousRunId, subtreeIds);
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
        return issues.stream()
                .map(issue -> new IssueKey(
                        issue.ruleId(), issue.objectType(), issue.objectId()))
                .collect(Collectors.toSet());
    }

    private static List<SeverityChange> severityChanges(List<IssueSummaryRow> currentIssues,
                                                        List<IssueSummaryRow> previousIssues) {
        Map<Severity, Long> currentCounts = countBySeverity(currentIssues);
        Map<Severity, Long> previousCounts = previousIssues == null ? null : countBySeverity(previousIssues);
        List<SeverityChange> changes = new ArrayList<>();
        for (Severity severity : Severity.values()) {
            long current = currentCounts.getOrDefault(severity, 0L);
            Long previous = previousCounts == null ? null : previousCounts.getOrDefault(severity, 0L);
            Long delta = previous == null ? null : current - previous;
            changes.add(new SeverityChange(severity, current, previous, delta));
        }
        return List.copyOf(changes);
    }

    private static Map<Severity, Long> countBySeverity(List<IssueSummaryRow> issues) {
        return issues.stream()
                .collect(Collectors.groupingBy(IssueSummaryRow::severity, Collectors.counting()));
    }

    private static RunReference reference(KkdRun run) {
        LocalDate snapshotDate = run.getSnapshotDate().atOffset(ZoneOffset.UTC).toLocalDate();
        return new RunReference(run.getId(), snapshotDate);
    }
}
