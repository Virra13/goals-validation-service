package ru.virra.kkd.domain.model;

/**
 * Состояние выполнения прогона ККД.
 * В качестве прогона для сравнения допускается только завершённый прогон.
 */
public enum RunStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
