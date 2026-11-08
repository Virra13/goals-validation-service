package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.virra.kkd.repository.projection.IssueSummaryRow;
import ru.virra.kkd.entity.KkdIssue;

import java.util.Collection;
import java.util.List;

/**
 * Репозиторий проблем, сохранённых по результатам прогонов ККД.
 * Предоставляет проекцию проблем для сводной статистики
 * и сравнения результатов прогонов.
 */
public interface KkdIssueRepository extends JpaRepository<KkdIssue, Long> {
    /**
     * Возвращает поля проблем, необходимые для сводной статистики и сравнения прогонов.
     * Описания и связанные сущности не загружаются.
     *
     * @param runId идентификатор прогона ККД
     * @param unitIds идентификаторы подразделений выбранного поддерева
     * @return строки с правилом, объектом и важностью каждой найденной проблемы
     */
    @Query("""
            select new ru.virra.kkd.repository.projection.IssueSummaryRow(
                i.rule.id, i.objectType, i.objectId, i.severity)
            from KkdIssue i where i.run.id = :runId and i.unitId in :unitIds
            """)
    List<IssueSummaryRow> findSummaryRows(@Param("runId") long runId,
                                        @Param("unitIds") Collection<Long> unitIds);
}
