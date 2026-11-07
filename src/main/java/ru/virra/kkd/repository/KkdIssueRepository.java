package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.virra.kkd.repository.projection.IssueSummaryRow;
import ru.virra.kkd.domain.model.IssueObjectType;
import ru.virra.kkd.domain.model.Severity;
import ru.virra.kkd.entity.KkdIssue;

import java.util.Collection;
import java.util.List;

public interface KkdIssueRepository extends JpaRepository<KkdIssue, Long> {
    @Query("""
            select new ru.virra.kkd.repository.projection.IssueSummaryRow(
                i.rule.id, i.objectType, i.objectId, i.severity)
            from KkdIssue i where i.run.id = :runId and i.unitId in :unitIds
            """)
    List<IssueSummaryRow> findSummaryRows(@Param("runId") long runId,
                                        @Param("unitIds") Collection<Long> unitIds);
    List<KkdIssue> findAllByRunId(Long runId);
    List<KkdIssue> findAllByRunIdAndUnitIdIn(long runId, Collection<Long> unitIds);
    List<KkdIssue> findAllByRunIdAndSeverity(Long runId, Severity severity);
    List<KkdIssue> findAllByRunIdAndRuleCode(Long runId, String ruleCode);
    boolean existsByRunIdAndRuleIdAndObjectTypeAndObjectId(Long runId, Long ruleId, IssueObjectType objectType, Long objectId);
    long countByRunId(Long runId);
}
