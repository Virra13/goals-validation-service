package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.virra.kkd.entity.Unit;
import ru.virra.kkd.repository.projection.UnitHierarchyRow;

import java.util.List;

/**
 * Репозиторий подразделений и их иерархии.
 * Предоставляет связи подразделений с родителями и рекурсивную выборку поддерева.
 */
public interface UnitRepository extends JpaRepository<Unit, Long> {

    /**
     * Получает связи всех подразделений с родительскими подразделениями.
     *
     * @return строки с идентификатором подразделения и идентификатором родителя;
     *         у подразделения верхнего уровня родитель равен {@code null}
     */
    @Query("""
            select new ru.virra.kkd.repository.projection.UnitHierarchyRow(u.id, p.id)
            from Unit u left join u.parent p
            """)
    List<UnitHierarchyRow> findHierarchy();

    /**
     * Находит подразделение и всех его потомков одним рекурсивным запросом.
     *
     * @param unitId идентификатор выбранного подразделения
     * @return идентификаторы подразделений поддерева или пустой список,
     *         если выбранное подразделение не найдено
     */
    @Query(value = """
            WITH RECURSIVE subtree(id) AS (
                SELECT id FROM unit WHERE id = :unitId
                UNION
                SELECT child.id
                FROM unit child
                JOIN subtree parent ON child.parent_id = parent.id
            )
            SELECT id FROM subtree
            """, nativeQuery = true)
    List<Long> findSubtreeIds(@Param("unitId") long unitId);
}
