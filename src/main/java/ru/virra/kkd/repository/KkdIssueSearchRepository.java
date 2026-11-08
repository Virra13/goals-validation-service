package ru.virra.kkd.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.domain.model.IssueSeverity;
import ru.virra.kkd.entity.KkdIssue;
import ru.virra.kkd.entity.KkdRule;
import ru.virra.kkd.readmodel.IssueListItem;
import ru.virra.kkd.readmodel.IssuePage;

import java.util.ArrayList;
import java.util.List;

/**
 * Выполняет поиск сохранённых проблем ККД по параметрам запроса страницы проблем.
 * Отдельно получает общее число подходящих записей и содержимое страницы;
 * для ответа выбирает только необходимые поля без загрузки связанных сущностей.
 */
@Repository
@Transactional(readOnly = true)
public class KkdIssueSearchRepository {
    private final EntityManager entityManager;

    public KkdIssueSearchRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Возвращает страницу проблем по возрастанию идентификатора.
     * Поле {@code total} содержит число записей, прошедших фильтры до пагинации.
     *
     * @param runId идентификатор прогона ККД
     * @param unitIds идентификаторы выбранного поддерева или {@code null} для всех подразделений
     * @param ruleCode код правила ККД или {@code null} без фильтра по правилу
     * @param severity важность проблемы или {@code null} без фильтра по важности
     * @param limit максимальное число проблем на странице
     * @param offset число подходящих проблем, пропускаемых перед страницей
     * @return страница проблем и общее число записей до применения пагинации
     */
    public IssuePage findPage(long runId, List<Long> unitIds, String ruleCode,
                              IssueSeverity severity, int limit, int offset) {
        long total = countIssues(runId, unitIds, ruleCode, severity);
        List<IssueListItem> content = loadIssues(runId, unitIds, ruleCode, severity, limit, offset);
        return new IssuePage(content, limit, offset, total);
    }

    private long countIssues(long runId, List<Long> unitIds, String ruleCode,
                             IssueSeverity severity) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<KkdIssue> issue = countQuery.from(KkdIssue.class);
        Join<KkdIssue, KkdRule> rule = issue.join("rule");
        Predicate[] conditions = filters(cb, issue, rule, runId, unitIds, ruleCode, severity);
        countQuery.select(cb.count(issue)).where(conditions);
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private List<IssueListItem> loadIssues(long runId, List<Long> unitIds, String ruleCode,
                                           IssueSeverity severity, int limit, int offset) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<IssueListItem> pageQuery = cb.createQuery(IssueListItem.class);
        Root<KkdIssue> issue = pageQuery.from(KkdIssue.class);
        Join<KkdIssue, KkdRule> rule = issue.join("rule");
        Predicate[] conditions = filters(cb, issue, rule, runId, unitIds, ruleCode, severity);
        pageQuery.select(cb.construct(IssueListItem.class,
                        issue.get("id"), rule.get("code"), issue.get("objectType"),
                        issue.get("objectId"), issue.get("severity"), issue.get("description")))
                .where(conditions)
                .orderBy(cb.asc(issue.get("id")));
        return entityManager.createQuery(pageQuery)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    private static Predicate[] filters(CriteriaBuilder cb, Root<KkdIssue> issue,
                                        Join<KkdIssue, KkdRule> rule, long runId,
                                        List<Long> unitIds, String ruleCode, IssueSeverity severity) {
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
