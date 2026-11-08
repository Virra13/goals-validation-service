package ru.virra.kkd.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.domain.metric.MetricKey;
import ru.virra.kkd.domain.metric.MetricValues;
import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.entity.KkdIssue;
import ru.virra.kkd.entity.KkdRule;
import ru.virra.kkd.entity.KkdRun;
import ru.virra.kkd.entity.KkdRunMetric;
import ru.virra.kkd.entity.Unit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Сохраняет найденные проблемы и рассчитанные метрики прогона через JPA.
 * Работает только внутри транзакции выполнения прогона. После каждых 250
 * результатов выполняет {@code flush} и отсоединяет сохранённые сущности,
 * сохраняя управляемую сущность прогона для изменения её статуса.
 */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class KkdResultWriter {
    // Limit retained result entities independently of Hibernate's JDBC batch size (50).
    private static final int FLUSH_INTERVAL = 250;
    private final EntityManager entityManager;

    public KkdResultWriter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Сохраняет проблемы, обнаруженные при проверке правил ККД.
     * Код правила преобразуется в идентификатор записи справочника,
     * а подразделение определяется по цели, к которой относится проблема.
     *
     * @param runId идентификатор текущего прогона ККД
     * @param findings проблемы, обнаруженные при проверке правил
     * @param unitByGoal соответствие идентификаторов целей и подразделений
     * @param ruleIds соответствие кодов правил и идентификаторов записей справочника
     * @param detectedAt время обнаружения проблем
     * @throws IllegalStateException если код правила отсутствует в справочнике
     */
    public void saveIssues(long runId, List<Finding> findings, Map<Long, Long> unitByGoal,
                           Map<String, Long> ruleIds, Instant detectedAt) {
        KkdRun run = entityManager.getReference(KkdRun.class, runId);
        List<KkdIssue> pending = new ArrayList<>(FLUSH_INTERVAL);
        for (Finding finding : findings) {
            Long ruleId = ruleIds.get(finding.ruleCode());
            if (ruleId == null) {
                throw new IllegalStateException("В справочнике ККД нет правила " + finding.ruleCode());
            }
            KkdIssue issue = newIssue(run, finding, ruleId, unitByGoal, detectedAt);
            entityManager.persist(issue);
            pending.add(issue);
            if (pending.size() == FLUSH_INTERVAL) {
                flushResults(pending);
            }
        }
        flushResults(pending);
    }

    /**
     * Сохраняет значения всех метрик для каждого подразделения.
     * При нулевом знаменателе значение метрики остаётся {@code null}.
     *
     * @param runId идентификатор текущего прогона ККД
     * @param values рассчитанные метрики по идентификаторам подразделений
     * @param calculatedAt время расчёта
     */
    public void saveMetrics(long runId, Map<Long, MetricValues> values, Instant calculatedAt) {
        KkdRun run = entityManager.getReference(KkdRun.class, runId);
        List<KkdRunMetric> pending = new ArrayList<>(FLUSH_INTERVAL);
        for (Map.Entry<Long, MetricValues> entry : values.entrySet()) {
            Unit unit = entityManager.getReference(Unit.class, entry.getKey());
            for (MetricKey key : MetricKey.values()) {
                KkdRunMetric metric = newMetric(run, unit, key, entry.getValue(), calculatedAt);
                entityManager.persist(metric);
                pending.add(metric);
                if (pending.size() == FLUSH_INTERVAL) {
                    flushResults(pending);
                }
            }
        }
        flushResults(pending);
    }

    private KkdIssue newIssue(KkdRun run, Finding finding, long ruleId,
                              Map<Long, Long> unitByGoal, Instant detectedAt) {
        KkdIssue issue = new KkdIssue();
        issue.setRun(run);
        issue.setRule(entityManager.getReference(KkdRule.class, ruleId));
        issue.setUnitId(unitByGoal.get(finding.goalId()));
        issue.setObjectType(finding.objectType());
        issue.setObjectId(finding.objectId());
        issue.setSeverity(finding.severity());
        issue.setDescription(finding.description());
        issue.setDetectedAt(detectedAt);
        return issue;
    }

    private static KkdRunMetric newMetric(KkdRun run, Unit unit, MetricKey key,
                                          MetricValues values, Instant calculatedAt) {
        KkdRunMetric metric = new KkdRunMetric();
        metric.setRun(run);
        metric.setUnit(unit);
        metric.setMetricCode(key.code());
        metric.setValue(key.valueOf(values));
        metric.setCalculatedAt(calculatedAt);
        return metric;
    }

    private void flushResults(List<?> pending) {
        if (pending.isEmpty()) {
            return;
        }
        entityManager.flush();
        // Do not clear the whole context: the run must remain managed for its COMPLETED update.
        pending.forEach(entityManager::detach);
        pending.clear();
    }
}
