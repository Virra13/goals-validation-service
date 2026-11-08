package ru.virra.kkd.repository.projection;

/**
 * Проекция связи подразделения с родительским подразделением.
 * У подразделения верхнего уровня {@code parentId} равен {@code null}.
 */
public record UnitHierarchyRow(long id, Long parentId) {
}
