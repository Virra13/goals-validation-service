package ru.virra.kkd.domain.comparison;

import ru.virra.kkd.domain.model.IssueObjectType;

/**
 * Ключ идентичности проблемы при сравнении прогонов ККД.
 * Состоит из идентификатора правила, типа объекта и идентификатора объекта.
 * Описание проблемы и уровень важности в ключ не входят.
 */
public record IssueKey(long ruleId, IssueObjectType objectType, long objectId) {
}
