package ru.virra.kkd.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.domain.model.Severity;
import ru.virra.kkd.entity.KkdIssue;
import ru.virra.kkd.entity.KkdRule;
import ru.virra.kkd.readmodel.IssueListItem;
import ru.virra.kkd.readmodel.IssuePage;

import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class KkdIssueSearchRepository {
    private final EntityManager entityManager;

    public KkdIssueSearchRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public IssuePage findPage(long runId, List<Long> unitIds, String ruleCode,
                              Severity severity, int limit, int offset) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<KkdIssue> counted = countQuery.from(KkdIssue.class);
        Join<KkdIssue, KkdRule> countedRule = counted.join("rule");
        countQuery.select(cb.count(counted)).where(
                filters(cb, counted, countedRule, runId, unitIds, ruleCode, severity));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<IssueListItem> pageQuery = cb.createQuery(IssueListItem.class);
        Root<KkdIssue> issue = pageQuery.from(KkdIssue.class);
        Join<KkdIssue, KkdRule> rule = issue.join("rule");
        pageQuery.select(cb.construct(IssueListItem.class,
                        issue.get("id"), rule.get("code"), issue.get("objectType"),
                        issue.get("objectId"), issue.get("severity"), issue.get("description")))
                .where(filters(cb, issue, rule, runId, unitIds, ruleCode, severity))
                .orderBy(cb.asc(issue.get("id")));
        List<IssueListItem> content = entityManager.createQuery(pageQuery)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
        return new IssuePage(content, limit, offset, total);
    }

    private static Predicate[] filters(CriteriaBuilder cb, Root<KkdIssue> issue,
                                        Join<KkdIssue, KkdRule> rule, long runId,
                                        List<Long> unitIds, String ruleCode, Severity severity) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(issue.get("run").get("id"), runId));
        if (unitIds != null) {
            predicates.add(issue.get("unitId").in(unitIds));
        }
        if (ruleCode != null) {
            predicates.add(cb.equal(rule.get("code"), ruleCode));
        }
        if (severity != null) {
            predicates.add(cb.equal(issue.get("severity"), severity));
        }
        return predicates.toArray(Predicate[]::new);
    }
}
