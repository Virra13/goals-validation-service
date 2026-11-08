package ru.virra.kkd.domain.model;

/**
 * Уровень важности проблемы ККД во внутренней модели приложения.
 * В БД значения хранятся в верхнем регистре, в HTTP API — в нижнем.
 */
public enum IssueSeverity {
    CRITICAL,
    MAJOR,
    MINOR
}
