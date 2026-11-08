package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.virra.kkd.entity.KkdRun;
import ru.virra.kkd.repository.projection.RunListRow;

import java.util.List;

/**
 * Репозиторий прогонов ККД.
 * Предоставляет проекцию для списка прогонов с количеством найденных проблем.
 */
public interface KkdRunRepository extends JpaRepository<KkdRun, Long> {
    /**
     * Возвращает проекции прогонов с количеством найденных проблем.
     * Прогоны без проблем включаются в результат с нулевым счётчиком.
     *
     * @return прогоны по убыванию даты снимка и идентификатора
     */
    @Query("""
            select new ru.virra.kkd.repository.projection.RunListRow(
                r.id, r.status, r.snapshotDate, base.id, count(i.id)
            )
            from KkdRun r
            left join r.comparedToRun base
            left join KkdIssue i on i.run = r
            group by r.id, r.status, r.snapshotDate, base.id
            order by r.snapshotDate desc, r.id desc
            """)
    List<RunListRow> findRunList();
}
