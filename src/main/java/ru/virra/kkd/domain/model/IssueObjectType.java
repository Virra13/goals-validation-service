package ru.virra.kkd.domain.model;

/**
 * Тип объекта, для которого обнаружена проблема ККД.
 * Используется вместе с идентификатором объекта при сравнении проблем между прогонами.
 */
public enum IssueObjectType {
    GOAL,
    KEY_RESULT
}
