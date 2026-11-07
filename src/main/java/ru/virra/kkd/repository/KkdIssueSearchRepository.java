package ru.virra.kkd.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.virra.kkd.domain.model.IssueObjectType;
import ru.virra.kkd.domain.model.Severity;
import ru.virra.kkd.readmodel.IssueListItem;
import ru.virra.kkd.readmodel.IssuePage;

import java.util.List;

@Repository
public class KkdIssueSearchRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public KkdIssueSearchRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public IssuePage findPage(long runId, List<Long> unitIds, String ruleCode,
                              Severity severity, int limit, int offset) {
        StringBuilder filter = new StringBuilder("""
                FROM kkd_issue i
                JOIN kkd_rule r ON r.id = i.rule_id
                WHERE i.run_id = :runId
                """);
        MapSqlParameterSource params = new MapSqlParameterSource("runId", runId);

        if (unitIds != null) {
            filter.append(" AND i.unit_id IN (:unitIds)");
            params.addValue("unitIds", unitIds);
        }
        if (ruleCode != null) {
            filter.append(" AND r.code = :ruleCode");
            params.addValue("ruleCode", ruleCode);
        }
        if (severity != null) {
            filter.append(" AND i.severity = :severity");
            params.addValue("severity", severity.name());
        }

        String fromAndWhere = filter.toString();
        long total = jdbc.queryForObject("SELECT count(*) " + fromAndWhere, params, Long.class);

        params.addValue("limit", limit);
        params.addValue("offset", offset);
        String pageSql = """
                SELECT i.id, r.code, i.object_type, i.object_id, i.severity, i.description
                """ + fromAndWhere + " ORDER BY i.id LIMIT :limit OFFSET :offset";

        List<IssueListItem> content = jdbc.query(pageSql, params, (rs, rowNumber) ->
                new IssueListItem(
                        rs.getLong("id"),
                        rs.getString("code"),
                        IssueObjectType.valueOf(rs.getString("object_type")),
                        rs.getLong("object_id"),
                        Severity.valueOf(rs.getString("severity")),
                        rs.getString("description")));

        return new IssuePage(content, limit, offset, total);
    }
}
