package ru.virra.kkd.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.domain.metric.MetricKey;
import ru.virra.kkd.domain.metric.MetricValues;
import ru.virra.kkd.domain.model.Finding;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class KkdResultWriter {
    private static final int BATCH_SIZE = 250;
    private final JdbcTemplate jdbc;

    public KkdResultWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void saveIssues(long runId, List<Finding> findings, Map<Long, Long> unitByGoal,
                           Map<String, Long> ruleIds, Instant detectedAt) {
        jdbc.batchUpdate("""
                INSERT INTO kkd_issue
                  (run_id, rule_id, unit_id, object_type, object_id, severity, description, detected_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, findings, BATCH_SIZE, (ps, finding) -> {
            Long ruleId = ruleIds.get(finding.ruleCode());
            if (ruleId == null) {
                throw new IllegalStateException("В справочнике ККД нет правила " + finding.ruleCode());
            }
            ps.setLong(1, runId);
            ps.setLong(2, ruleId);
            ps.setLong(3, unitByGoal.get(finding.goalId()));
            ps.setString(4, finding.objectType().name());
            ps.setLong(5, finding.objectId());
            ps.setString(6, finding.severity().name());
            ps.setString(7, finding.description());
            ps.setTimestamp(8, Timestamp.from(detectedAt));
        });
    }

    public void saveMetrics(long runId, Map<Long, MetricValues> values, Instant calculatedAt) {
        List<MetricRow> rows = new ArrayList<>();
        values.forEach((unitId, metrics) -> {
            for (MetricKey key : MetricKey.values()) rows.add(new MetricRow(unitId, key, metrics));
        });
        jdbc.batchUpdate("""
                INSERT INTO kkd_run_metric (run_id, unit_id, metric_code, value, calculated_at)
                VALUES (?, ?, ?, ?, ?)
                """, rows, BATCH_SIZE, (ps, row) -> {
            ps.setLong(1, runId);
            ps.setLong(2, row.unitId());
            ps.setString(3, row.key().code());
            var value = row.key().valueOf(row.values());
            if (value == null) ps.setNull(4, Types.NUMERIC);
            else ps.setBigDecimal(4, value);
            ps.setTimestamp(5, Timestamp.from(calculatedAt));
        });
    }

    private record MetricRow(long unitId, MetricKey key, MetricValues values) {
    }
}
